package com.alexmclain.duet.pjlink.dr1_0_0;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * PJLink Interface
 * 
 * @author Alex McLain <alex@alexmclain.com>
 *
 * REFER TO THE PJLINK SPECIFICATION:
 * http://pjlink.jbmia.or.jp/english/data/PJLink%20Specifications100.pdf
 */
public class PJLink {
	// Projector errors.
	public static final int ERROR_UNDEFINED_COMMAND	= 1;
	public static final int ERROR_UNAVAILABLE_TIME	= 3;
	public static final int ERROR_PROJECTOR_FAILURE	= 4;
	
	// Power codes taken from PJLink spec.
	public static final int POWER_OFF			= 0;
	public static final int POWER_ON			= 1;
	public static final int POWER_COOLING		= 2;
	public static final int POWER_WARMING		= 3;
	
	// Input codes taken from PJLink spec.
	public static final int INPUT_ERROR_NONEXISTENT_SOURCE	= 2;
	
	public static final int INPUT_RGB_1			= 11;
	public static final int INPUT_RGB_2			= 12;
	public static final int INPUT_RGB_3			= 13;
	public static final int INPUT_RGB_4			= 14;
	public static final int INPUT_RGB_5			= 15;
	public static final int INPUT_RGB_6			= 16;
	public static final int INPUT_RGB_7			= 17;
	public static final int INPUT_RGB_8			= 18;
	public static final int INPUT_RGB_9			= 19;
	
	public static final int INPUT_VIDEO_1		= 21;
	public static final int INPUT_VIDEO_2		= 22;
	public static final int INPUT_VIDEO_3		= 23;
	public static final int INPUT_VIDEO_4		= 24;
	public static final int INPUT_VIDEO_5		= 25;
	public static final int INPUT_VIDEO_6		= 26;
	public static final int INPUT_VIDEO_7		= 27;
	public static final int INPUT_VIDEO_8		= 28;
	public static final int INPUT_VIDEO_9		= 29;
	
	public static final int INPUT_DIGITAL_1		= 31;
	public static final int INPUT_DIGITAL_2		= 32;
	public static final int INPUT_DIGITAL_3		= 33;
	public static final int INPUT_DIGITAL_4		= 34;
	public static final int INPUT_DIGITAL_5		= 35;
	public static final int INPUT_DIGITAL_6		= 36;
	public static final int INPUT_DIGITAL_7		= 37;
	public static final int INPUT_DIGITAL_8		= 38;
	public static final int INPUT_DIGITAL_9		= 39;
	
	public static final int INPUT_STORAGE_1		= 41;
	public static final int INPUT_STORAGE_2		= 42;
	public static final int INPUT_STORAGE_3		= 43;
	public static final int INPUT_STORAGE_4		= 44;
	public static final int INPUT_STORAGE_5		= 45;
	public static final int INPUT_STORAGE_6		= 46;
	public static final int INPUT_STORAGE_7		= 47;
	public static final int INPUT_STORAGE_8		= 48;
	public static final int INPUT_STORAGE_9		= 49;
	
	public static final int INPUT_NETWORK_1		= 51;
	public static final int INPUT_NETWORK_2		= 52;
	public static final int INPUT_NETWORK_3		= 53;
	public static final int INPUT_NETWORK_4		= 54;
	public static final int INPUT_NETWORK_5		= 55;
	public static final int INPUT_NETWORK_6		= 56;
	public static final int INPUT_NETWORK_7		= 57;
	public static final int INPUT_NETWORK_8		= 58;
	public static final int INPUT_NETWORK_9		= 59;
	
	// A/V Mute codes taken from PJLink spec.
	public static final int MUTE_ERROR_CANNOT_MUTE	= 2;
	
	public static final int MUTE_VIDEO_ONLY		= 11;
	public static final int MUTE_AUDIO_ONLY		= 21;
	public static final int MUTE_AUDIO_VIDEO	= 31;
	public static final int MUTE_OFF			= 30;
	
	////////////////////////////////////////////////////////////
	
	// PJLink connection.
	private int _TCPPort = 4352;
	private String _ipAddress = "";
	private String _pjlinkPassword = "";
	
	private PJLinkSocket _pjlinkSocket = new PJLinkSocket();
	private PJLinkQueue _pjlinkQueue = new PJLinkQueue();
	private PJLinkRefreshTimer _rft = new PJLinkRefreshTimer();
	
	private ArrayList _pjlinkListeners = new ArrayList();
	
	// Device state.
	private int _powerState = POWER_OFF;
	private int _newPowerState = POWER_OFF;
	
	private int _activeInput = INPUT_RGB_1;
	private int _newActiveInput = INPUT_RGB_1;
	
	private boolean _audioMuteActive = false;
	private boolean _audioMuteStateBeforeVideoMuted = false;
	private boolean _newAudioMuteActive = false;
	
	private boolean _videoMuteActive = false;
	private boolean _newVideoMuteActive = false;
	
	private int _lampHours = 0;
	
	////////////////////////////////////////////////////////////
	
	public PJLink() {
	}
	
	public PJLink(String ipAddress) {
		setIPAddress(ipAddress);
	}
	
	public PJLink(String ipAddress, int port) {
		setIPAddress(ipAddress);
		setPort(port);
	}
	
	public void addListener(PJLinkListener listener) {
		if (_pjlinkListeners.contains(listener) == true) return;
		_pjlinkListeners.add(listener);
	}
	
	public void removeListener(PJLinkListener listener) {
		_pjlinkListeners.remove(listener);
	}
	
	private void notifyListeners(PJLinkEvent event) {
		Iterator i = _pjlinkListeners.iterator();
		while (i.hasNext()) {
			((PJLinkListener) i.next()).deviceStateChanged(this, event);
		}
	}
	
	public String getIPAddress() {
		return _ipAddress;
	}
	
	public int getPort() {
		return _TCPPort;
	}
	
	public int getPowerState() {
		queryPowerState();
		return _powerState;
	}
	
	
	public void powerOn() {
		_pjlinkQueue.push(new PJLinkCommand("%1POWR 1"));
		if (_powerState == POWER_OFF) _newPowerState = POWER_WARMING;
		queryPowerState();
	}
	
	public void powerOff() {
		_pjlinkQueue.push(new PJLinkCommand("%1POWR 0"));
		if (_powerState == POWER_ON) _newPowerState = POWER_COOLING;
		queryPowerState();
	}
	
	public void switchInput(int input) {
		if (input < 11 || input > 59) return;
		
		_pjlinkQueue.push(new PJLinkCommand("%1INPT " + input));
		_newActiveInput = input;
	}
	
	public void muteAudio() {
		if (_audioMuteActive == true) return;
		_newAudioMuteActive = true;
		sendAVMuteState();
	}
	
	public void unmuteAudio() {
		if (_audioMuteActive == false) return;
		if (_videoMuteActive == false) _newAudioMuteActive = false;
		sendAVMuteState();
	}
	
	public void muteVideo() {
		if (_videoMuteActive == true) return;
		
		// Muting video also mutes audio, but the audio mute
		// state must be retained when video is unmuted again.
		if (_videoMuteActive == false) _audioMuteStateBeforeVideoMuted = _audioMuteActive;
		
		_newAudioMuteActive = true;
		_newVideoMuteActive = true;
		sendAVMuteState();
	}
	
	public void unmuteVideo() {
		if (_videoMuteActive == false) return;
		_newAudioMuteActive = _audioMuteStateBeforeVideoMuted;
		_newVideoMuteActive = false;
		sendAVMuteState();
	}
	
	private void sendAVMuteState() {
		if (_newVideoMuteActive == true) {
			// Mute audio and video.
			_pjlinkQueue.push(new PJLinkCommand("%1AVMT 31"));
		}
		else {
			if (_newAudioMuteActive == true) {
				// Unmute video, leave audio muted.
				_pjlinkQueue.push(new PJLinkCommand("%1AVMT 10"));
				_pjlinkQueue.push(new PJLinkCommand("%1AVMT 21"));
			}
			else {
				// Unmute audio and video.
				_pjlinkQueue.push(new PJLinkCommand("%1AVMT 30"));
			}
		}

		queryAVMute();
	}
	
	
	public void queryAll() {
		queryErrorStatus();
		queryPowerState();
		queryInput();
		queryAVMute();
		queryLampHours();
	}
	
	public void queryAVMute() {
		_pjlinkQueue.push(new PJLinkCommand("%1AVMT ?"));
	}
	
	public void queryErrorStatus() {
		_pjlinkQueue.push(new PJLinkCommand("%1ERST ?"));
	}
	
	public void queryInput() {
		_pjlinkQueue.push(new PJLinkCommand("%1INPT ?"));
	}
	
	public void queryInputList() {
		_pjlinkQueue.push(new PJLinkCommand("%1INST ?"));
	}
	
	public void queryPowerState() {
		_pjlinkQueue.push(new PJLinkCommand("%1POWR ?"));
	}
	
	public void queryLampHours() {
		
	}
	
	
	/**
	 * 
	 * @param password
	 * 
	 * Setting a password automatically sets the module to use authentication.
	 * Clearing the password disables the PJLink authentication procedure.
	 */
	public void setPassword(String password) {
		_pjlinkPassword = password;
	}
	
	public void setIPAddress(String ipAddress) {
		_ipAddress = ipAddress;
		if (_ipAddress.length() != 0) queryAll();
	}
	
	public void setPort(int port) {
		_TCPPort = port;
	}
	
	private void updatePowerState() {
		notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_POWER, _powerState));
	}
	
	private void updateInputState() {
		notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_INPUT, _activeInput));
	}
	
	private void updateAVMuteState() {
		int muteState = MUTE_OFF;
		
		if (_videoMuteActive == true && _audioMuteActive == true) {
			muteState = MUTE_AUDIO_VIDEO;
		}
		else if (_videoMuteActive == true && _audioMuteActive == false) {
			muteState = MUTE_VIDEO_ONLY;
		}
		else if (_videoMuteActive == false && _audioMuteActive == true) {
			muteState = MUTE_AUDIO_ONLY;
		}
		
		notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_AV_MUTE, muteState));
	}
	
	
	/**
	 * 
	 *
	 */
	private class PJLinkRefreshTimer {
		Timer _pjlinkRefreshTimer = new Timer(true);
		int x = 0;
		
		public PJLinkRefreshTimer () {
			_pjlinkRefreshTimer.scheduleAtFixedRate(new TimerTask() {

				public void run() {
					if (_ipAddress.length() != 0) queryAll();
				}
				
			}, 5000, 5000);
			
		}
	}
	
	/**
	 * 
	 *
	 */
	private class PJLinkQueue {
		ArrayList _commandQueue = new ArrayList();
		
		private Thread _queueThread = new Thread(new PJLinkQueueRunner());
		
		public PJLinkQueue() {
			_queueThread.start();
		}
		
		public synchronized void push(PJLinkCommand command) {
			_commandQueue.add(command);
		}
		
		private synchronized PJLinkCommand pop() {
			if (_commandQueue.isEmpty() == true) return null;
			return (PJLinkCommand) _commandQueue.remove(0);
		}
		
		public synchronized boolean isEmpty() {
			return _commandQueue.isEmpty();
		}
		
		private class PJLinkQueueRunner implements Runnable {
			
			public void run() {
				while (true) {
					if (_commandQueue.isEmpty() == true) {
						Thread.yield();
					}
					else {
						PJLinkCommand command = (PJLinkCommand) pop();
						if (command != null) command.execute();
					}
				}
			}
		}
	}
	
	private class PJLinkSocket {		
		private String _pjlinkKey = "";			// Random number generated by PJLink upon connect.
		
		private Socket _socket = new Socket();
		private BufferedReader _socketReader;
		private PrintWriter _socketWriter;
		private Thread _socketThread;
		private SocketDataListener _socketDataListener;
		
		private MessageDigest _md5;
		
		private boolean _sessionUsesAuthentication = false;
		
		////////////////////////////////////////
		// TODO: Turn this into a state machine.
		////////////////////////////////////////
		private boolean _socketReadyForCommand = false;		// Projector has responded after opening socket.
		private boolean _socketCommandReceived = true;
		
		private Object _socketLock = new Object();
		
		public PJLinkSocket() {
			try {
				_md5 = MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException e) {
				System.out.println("PJLink error: MD5 encryption not supported. Disable authentication.");
			}
		}
		
		/**
		 * 
		 * @param command
		 * 
		 * This method is already called from another thread.
		 */
		public void sendCommand(String command) {
			synchronized (_socketLock) {
				// Expires the connection if it stalls.
				Timer connectionExpire = new Timer(false);
				connectionExpire.schedule(new TimerTask() {
					public void run() {
						disconnect();
						this.cancel();
					}
				}, 2000);
				
				connect();
				
				while (_socketReadyForCommand == false) Thread.yield();
				
				if (_sessionUsesAuthentication == true) {
					String pjlinkHash = new String(_md5.digest(new String(_pjlinkKey + " " + _pjlinkPassword).getBytes()));
					_socketWriter.println(_md5.digest(pjlinkHash.getBytes()) + command);
				}
				else {
					_socketWriter.println(command);
				}
				
				while (_socketCommandReceived == false) Thread.yield();
				
				connectionExpire.cancel();
			}
		}
		
		private void connect() {
			if (_ipAddress.length() == 0) {
				// Prevent thread from yielding on error.
				_socketReadyForCommand = true;
				_socketCommandReceived = true;
				return;
			}
			
			if (_socket != null) {
				if (_socket.isClosed() == false) {
					disconnect();
				}
			}
			
			try {
				_socket = new Socket(_ipAddress, _TCPPort);
				_socket.setTcpNoDelay(true);
				_socketReader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
				_socketWriter = new PrintWriter(_socket.getOutputStream(), true);
				_sessionUsesAuthentication = false;
				_socketReadyForCommand = false;
				_socketCommandReceived = false;
				_pjlinkKey = "";
				
				_socketDataListener = new SocketDataListener();
				_socketThread = new Thread(_socketDataListener);
				_socketThread.start();
			}
			catch (IOException ex) {
				System.out.println("PJLink connect error: " + ex.getLocalizedMessage());
			}
		}
		
		private void disconnect() {
			try {
				_socketReadyForCommand = true;
				_socketCommandReceived = true;
				
				_socket.close();
				_socketReader = null;
				_socketWriter = null;
				_socketCommandReceived = true;
			}
			catch (IOException ex) {
				// Don't care.  Socket is getting destroyed.
			}
		}
		
		/***********************************************************
		 	Socket Parser
		***********************************************************/
		private class SocketDataListener implements Runnable {
			
			public void run() {
				if (_socketReader == null) return;
				
				String line;
				
				try {
					while (	_socketReader != null &&
							_socket != null &&
							_socket.isClosed() == false &&
							(line = _socketReader.readLine()) != null) {
						
						// TODO: Remove
						/*
						System.out.println("PJLink received: " + line); // DEBUG /////////////////////////////////////////////////////////////////////////////////////
						System.out.flush();
						*/
						
						// Projector greeting, no authentication.
						if (line.startsWith("PJLINK 0")) {
							_pjlinkKey = "";
							_sessionUsesAuthentication = false;
							_socketReadyForCommand = true;
						}
						
						// Projector greeting, authentication challenge.
						else if (line.startsWith("PJLINK 1 ")) {
							_pjlinkKey = line.substring(9);
							_sessionUsesAuthentication = true;
							_socketReadyForCommand = true;
						}
						
						// PJLink data response.
						else {
							// Authentication error.
							if (line.indexOf(" ERRA") > -1) {
								System.out.println("PJLink authentication error. " + _ipAddress);
								_socket.close();
							}
							
							// Undefined command.
							if (line.endsWith("ERR1")){									
								notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, PJLink.ERROR_UNDEFINED_COMMAND));
							}
							
							// Unavailable time.
							if (line.endsWith("ERR3")){									
								notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, PJLink.ERROR_UNAVAILABLE_TIME));
							}
							
							// Projector failure.
							else if (line.endsWith("ERR4")) {
								notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, PJLink.ERROR_PROJECTOR_FAILURE));
							}
							
							// Power response.
							else if (line.startsWith("%1POWR=")) {
								
								// Accepted power command.
								if (line.endsWith("OK")) {
									_powerState = _newPowerState;
									updatePowerState();
								}
								
								// Returned power state value.
								else {
									int powerState = Integer.parseInt(line.substring(7));
									_powerState = powerState;
									updatePowerState();
								}
							}
							
							// Input response.
							else if (line.startsWith("%1INPT=")) {
								
								// Accepted input selection.
								if (line.endsWith("OK")) {
									// TODO: Implement input response.
									_activeInput = _newActiveInput;
									updateInputState();
								}
								
								// Nonexistent input source.
								else if (line.endsWith("ERR2")) {
									_newActiveInput = _activeInput; // Input switch cancelled.
									notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_INPUT, PJLink.INPUT_ERROR_NONEXISTENT_SOURCE));
								}
								
								// Returned active input value.
								else {
									int activeInput = Integer.parseInt(line.substring(7, 8));
									
									if (activeInput > 0) {
										_activeInput = activeInput;
									}
									
									updateInputState();
								}
							}
							
							// A/V mute response.
							else if (line.startsWith("%1AVMT=")) {								
								// Accepted mute instruction.
								if (line.endsWith("OK")) {
									_audioMuteActive = _newAudioMuteActive;
									_videoMuteActive = _newVideoMuteActive;
									updateAVMuteState();
								}
								else if (line.endsWith("ERR2")) {
									_newAudioMuteActive = _audioMuteActive;
									_newVideoMuteActive = _videoMuteActive;
									notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_AV_MUTE, PJLink.MUTE_ERROR_CANNOT_MUTE));
								}
								else {
									int avmt = Integer.parseInt(line.substring(7,9));
									
									switch (avmt) {
									
									case MUTE_VIDEO_ONLY:
										_videoMuteActive = true;
										_audioMuteActive = false;
										break;
									
									case MUTE_AUDIO_ONLY:
										_videoMuteActive = false;
										_audioMuteActive = true;
										break;
										
									case MUTE_AUDIO_VIDEO:
										_videoMuteActive = true;
										_audioMuteActive = true;
										break;
										
									case MUTE_OFF:
										_videoMuteActive = false;
										_audioMuteActive = false;
										break;
										
									default: break;
									}
									
									updateAVMuteState();
								}
							}
							
							// Error status response.
							else if (line.startsWith("%1ERST=")) {
								// TODO: Implement error status response.
								
								if (line.length() == 13) {
									int fanError = Integer.parseInt(line.substring(7, 8));
									int lampError = Integer.parseInt(line.substring(8, 9));
									int tempError = Integer.parseInt(line.substring(9, 10));
									int coverError = Integer.parseInt(line.substring(10, 11));
									int filterError = Integer.parseInt(line.substring(11, 12));
									int otherError = Integer.parseInt(line.substring(12, 13));
									
									// TODO: Remove
									/*
									System.out.println(
											"Fan error: " + fanError + "\n" +
											"Lamp error: " + lampError + "\n" +
											"Temp error: " + tempError + "\n" +
											"Cover error: " + coverError + "\n" +
											"Filter error: " + filterError + "\n" +
											"Other error: " + otherError
									);
									*/
									
									notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, fanError + lampError + tempError + coverError + filterError + otherError));
								}
							}
							
							// Lamp status response.
							else if (line.startsWith("%1LAMP=")) {
								// TODO: Implement lamp status response.
							}
							
							// Input list response.
							else if (line.startsWith("%1INST=")) {
								// TODO: Implement list enumeration response.
							}
							
							_socketCommandReceived = true;
						}
					}
					
					if (_socket != null) disconnect();
				}
				catch (IOException ex) {
					
				}
			}
		}
	}
	
	private class PJLinkCommand {
		private String _command = "";
		
		public PJLinkCommand() {
		}
		
		public PJLinkCommand(String command) {
			_command = command;
		}
		
		public void execute() {
			if (_command.length() == 0) return;
			if (_ipAddress.length() == 0) return;
			
			// TODO: Remove
			/*
			System.out.println("Executing command: " + _command);
			System.out.flush();
			*/
			
			_pjlinkSocket.sendCommand(_command);
		}
	}
}
