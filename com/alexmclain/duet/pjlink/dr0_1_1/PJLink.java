package com.alexmclain.duet.pjlink.dr0_1_1;

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
 * PJLink is a class for Java devices to interface with video projectors
 * that implement the PJLink protocol over TCP/IP. This class adheres
 * to the Java 1.4 API to maintain compatibility with AMX Duet devices.
 * <p>
 * The PJLink specification can be obtained at:<br>
 * <a href="http://pjlink.jbmia.or.jp/english/data/PJLink%20Specifications100.pdf">
 * http://pjlink.jbmia.or.jp/english/data/PJLink%20Specifications100.pdf</a>
 * <p>
 * This class queues requests to the projector and only keeps the network
 * socket open for the duration of each request/response cycle.
 * 
 * @author Alex McLain <alex@alexmclain.com>
 * @version 0.1.1
 */
public class PJLink {
	//	 Packed error bits:
	// | 15 | 14 | 13 | 12 | 11 | 10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
	// |Fail|Unav|Undf|Conn|  Other  | Filter| Cover |  Temp |  Lamp |  Fan  |
	
	// Projector errors.
	// See SocketDataListener private class for bit map.
	public static final int ERROR_FAN_WARNING		= 0x0001;
	public static final int ERROR_FAN_ERROR			= 0x0002;
	public static final int ERROR_LAMP_WARNING		= 0x0004;
	public static final int ERROR_LAMP_ERROR		= 0x0008;
	public static final int ERROR_TEMP_WARNING		= 0x0010;
	public static final int ERROR_TEMP_ERROR		= 0x0020;
	public static final int ERROR_COVER_WARNING		= 0x0040;
	public static final int ERROR_COVER_ERROR		= 0x0080;
	public static final int ERROR_FILTER_WARNING	= 0x0100;
	public static final int ERROR_FILTER_ERROR		= 0x0200;
	public static final int ERROR_OTHER_WARNING		= 0x0400;
	public static final int ERROR_OTHER_ERROR		= 0x0800;
	
	public static final int ERROR_CONNECTION		= 0x1000;
	public static final int ERROR_UNDEFINED_COMMAND	= 0x2000;
	public static final int ERROR_UNAVAILABLE_TIME	= 0x4000;
	public static final int ERROR_PROJECTOR_FAILURE	= 0x8000;
	
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
	
	boolean _connectionError = false;
	
	int _fanError = 0;
	int _lampError = 0;
	int _tempError = 0;
	int _coverError = 0;
	int _filterError = 0;
	int _otherError = 0;
	
	// Module settings.
	boolean _printDebug 	= false;	// Print debug statements to the console.
	boolean _disablePolling	= false;	// Disable polling of the projector state.
	long _refreshInterval	= 5;		// Number of seconds between polling of the projector state.
	
	////////////////////////////////////////////////////////////
	
	/**
	 * Creates an uninitialized PJLink connection.
	 * At a minimum, the projector's IP address must be specified
	 * before a connection can be established.
	 */
	public PJLink() {
	}
	
	/**
	 * Creates a PJLink connection to a projector at the specified IP address.
	 * The default PJLink port is used.
	 * @param ipAddress
	 */
	public PJLink(String ipAddress) {
		setIPAddress(ipAddress);
	}
	
	/**
	 * Creates a PJLink connection to a projector at the specified IP address and port.
	 * @param ipAddress
	 * @param port
	 */
	public PJLink(String ipAddress, int port) {
		setIPAddress(ipAddress);
		setPort(port);
	}
	
	/**
	 * Adds a <code>PJLinkListener</code> to receive changes regarding the projector's state.
	 * @param listener
	 */
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
	
	/**
	 * @return Projector is on, off, warming, or cooling.
	 */
	public int getPowerState() {
		queryPowerState();
		return _powerState;
	}
	
	public boolean getConnectionError() {
		return _connectionError;
	}
	
	public boolean getPrintDebug() {
		return _printDebug;
	}
	
	public boolean getDisablePolling() {
		return _disablePolling;
	}
	
	public long getRefreshInterval() {
		return _refreshInterval;
	}
	
	
	public void powerOn() {
		_pjlinkQueue.push(new PJLinkCommand("%1POWR 1"));
		if (_powerState == POWER_OFF) {
			_newPowerState = (_disablePolling == false) ? POWER_WARMING : POWER_ON;
		}
		if (_disablePolling == false) queryPowerState();
	}
	
	public void powerOff() {
		_pjlinkQueue.push(new PJLinkCommand("%1POWR 0"));
		if (_powerState == POWER_ON) {
			_newPowerState = (_disablePolling == false) ? POWER_COOLING : POWER_OFF;
		}
		if (_disablePolling == false) queryPowerState();
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

		if (_disablePolling == false) queryAVMute();
	}
	
	
	/**
	 * Queries projector error status, power state, selected input,
	 * A/V mute, and lamp hours. It does <i>not</i> query the input
	 * list.
	 */
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
		_pjlinkQueue.push(new PJLinkCommand("%1LAMP ?"));
	}
	
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
	
	public void setPrintDebug(boolean value) {
		_printDebug = value;
	}
	
	public void setDisablePolling(boolean value) {
		_disablePolling = value;
	}
	
	public void setRefreshInterval(long value) {
		_refreshInterval = value;
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
	 * The refresh timer queries all of the projector's parameters
	 * at a regular interval. This keeps the instance variables and
	 * class's listeners up to date without the need to poll the class.
	 */
	private class PJLinkRefreshTimer {
		Timer _pjlinkRefreshTimer = new Timer(true);
		int x = 0;
		
		public PJLinkRefreshTimer () {
			_pjlinkRefreshTimer.scheduleAtFixedRate(new TimerTask() {

				public void run() {
					if (_ipAddress.length() != 0 && _disablePolling == false) queryAll();
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
						_connectionError = true;
						
						if (_printDebug == true) System.out.println("PJLink connection timed out. " + _ipAddress);
						
						disconnect();
						notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, PJLink.ERROR_CONNECTION));
						this.cancel();
					}
				}, 4000);
				
				try {
					connect();
					
					while (_socketReadyForCommand == false) Thread.yield();
					
					if (_socketWriter != null) {	// This prevents the Duet module from crashing.
						if (_sessionUsesAuthentication == true) {
							String pjlinkHash = new String(_md5.digest(new String(_pjlinkKey + " " + _pjlinkPassword).getBytes()));
							_socketWriter.print(_md5.digest(pjlinkHash.getBytes()) + command + "\r");
						}
						else {
							_socketWriter.print(command + "\r");
						}
						
						_socketWriter.flush();
						
						while (_socketCommandReceived == false) Thread.yield();
					}
				}
				catch (IOException ex) {
					_connectionError = true;
					notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, PJLink.ERROR_CONNECTION));
					
					if (_printDebug == true) System.out.println("PJLink connection error. " + _ipAddress);
				}
				catch (Exception ex) {
					if (_printDebug == true) System.out.println("Unknown PJLink connection error. " + _ipAddress);
				}

				
				connectionExpire.cancel();
				
				boolean oldConnectionErrorState = _connectionError;
				_connectionError = false;
				
				if (oldConnectionErrorState == true) notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, PJLink.ERROR_CONNECTION));
			}
		}
		
		private void connect() throws IOException {
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
		
		private void disconnect() {
			try {
				_socket.close();
				_socketReader = null;
				_socketWriter = null;
				_socketCommandReceived = true;
				
				_socketReadyForCommand = true;
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
				String line;
				
				try {
					while (	_socketReader != null &&
							_socket != null &&
							_socket.isClosed() == false &&
							(line = _socketReader.readLine()) != null) {
						
						if (_printDebug == true) {
							System.out.println("PJLink received: " + line);
							System.out.flush();
						}
						
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
								if (line.length() == 13) {
									_fanError = Integer.parseInt(line.substring(7, 8));
									_lampError = Integer.parseInt(line.substring(8, 9));
									_tempError = Integer.parseInt(line.substring(9, 10));
									_coverError = Integer.parseInt(line.substring(10, 11));
									_filterError = Integer.parseInt(line.substring(11, 12));
									_otherError = Integer.parseInt(line.substring(12, 13));
									
									// TODO: Refactor error status bit packing.
									//       This method will just confuse people who are unfamiliar with binary operations.
									//       Make it easy and well-named.
									
									// Packed bits:
									// | 15 | 14 | 13 | 12 | 11 | 10 | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
									// |Fail|Unav|Undf|    |  Other  | Filter| Cover |  Temp |  Lamp |  Fan  |
									
									// Bit 15: Projector Failure
									// Bit 14: In Unavailable Time
									// Bit 13: Undefined Command
									
									int packedData = 	_fanError		<< 0 +
														_lampError		<< 2 + 
														_tempError		<< 4 +
														_coverError		<< 6 +
														_filterError	<< 8 +
														_otherError		<< 10;
									
									notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_ERROR, packedData));
								}
							}
							
							// Lamp status response.
							else if (line.startsWith("%1LAMP=")) {
								int hoursEndPos = line.indexOf(' ', 7);
								
								if (hoursEndPos > -1) {
									_lampHours = Integer.parseInt(line.substring(7, hoursEndPos));
									notifyListeners(new PJLinkEvent(PJLinkEvent.EVENT_LAMP, _lampHours));
								}
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
			
			if (_printDebug == true) {
				System.out.println("Executing command: " + _command);
				System.out.flush();
			}
			
			_pjlinkSocket.sendCommand(_command);
		}
	}
}
