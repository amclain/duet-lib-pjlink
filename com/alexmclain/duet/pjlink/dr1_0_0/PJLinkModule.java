package com.alexmclain.duet.pjlink.dr1_0_0;

import java.util.Properties;

import org.osgi.framework.BundleContext;

import com.amx.duet.core.master.netlinx.Custom;
import com.amx.duet.core.master.netlinx.DPS;
import com.amx.duet.core.master.netlinx.Event;
import com.amx.duet.core.master.netlinx.Level;
import com.amx.duet.da.NetLinxDevice;
import com.amx.duet.devicesdk.Utility;
import com.amx.duet.util.Timer;

/**
 * PJLink Duet Module
 * @version 0.1.1
 * 
 * @author Alex McLain <alex@alexmclain.com>
 *
 * The PJLinkModule class is the NetLinx entry point.
 * 
 * Refer to the PJLink specification:
 * http://pjlink.jbmia.or.jp/english/data/PJLink%20Specifications100.pdf
 * 
 * NOTE:
 * The "COMMAND CONTROL" setting in the projector's network setup menu must be DISABLED.
 * 
 * Interface tries to maintain compatability with the Panasonic EZ750 serial Duet module.
 * 
 * **************************************************
 * 
 * Channel:
 * 		9	Cycle Lamp Power
 * 		27	Set Lamp Power On
 * 		28	Set Lamp Power Off
 * 
 * 		199	Set Volume Mute
 * 		211	Set Picture Mute	- Also mutes audio.
 * 		214	Set Freeze
 * 
 * 		251	Device Is Online (Feedback)
 * 		252	Data Is Initialized (Feedback)
 * 		253	Projector Warming (Feedback)
 * 		254 Projector Cooling (Feedback)
 * 		255 Set Lamp Power On (Feedback) -- Disconnected from input.
 * 
 * Extended Channel:
 *		// Select Input
 *
 *		311-359	Correspond to PJLink "INPT" values 11-59.
 * 
 * 		// Errors And Warnings
 * 		// (Feedback Only)
 * 		499	Connection Error	
 * 
 * 		500	Fan Warning
 * 		501	Fan Error
 * 		502	Lamp Warning
 * 		503	Lamp Error
 * 		504	Temperature Warning
 * 		505	Temperature Error
 * 		506	Cover Warning
 * 		507	Cover Error
 * 		508	Filter Warning
 * 		509	Filter Error
 * 		510	Other Warning
 * 		511	Other Error
 * 		512	Projector Failure
 * 
 * Commands:
 * 		?LAMPTIME
 * 
 * Extended Commands:
 * 		IPADDR				- Set IP address.
 * 		DEBUG				- Print debug info to console.
 * 		DISABLE_POLLING		- Disable polling the projector periodically for its state.
 * 		REFRESH_INTERVAL	- Set how frequently the projector's state is polled.
 * 		
 * 		?CONN				- Query connection status.
 * 		?DEBUG 
 * 		?DISABLE_QUERY
 * 		?REFRESH_INTERVAL
 */
public class PJLinkModule extends Utility implements PJLinkListener{
	
	public static final int CHAN_AUDIO_MUTE		= 199;
	public static final int CHAN_PICTURE_MUTE	= 211;
	public static final int CHAN_FREEZE			= 214;
	public static final int CHAN_DEVICE_IS_INITIALIZED	= 251;
	public static final int CHAN_DATA_IS_INITIALIZED	= 252;
	public static final int CHAN_WARMING		= 253;
	public static final int CHAN_COOLING		= 254;
	public static final int CHAN_LAMP			= 255;
	
	/***********************************************************
	 	EXTENDED CHANNELS
	***********************************************************/
	
	// Input Selection And Indicators
	public static final int CHAN_INPUT_RGB_1		= 311;
	public static final int CHAN_INPUT_RGB_2		= 312;
	public static final int CHAN_INPUT_RGB_3		= 313;
	public static final int CHAN_INPUT_RGB_4		= 314;
	public static final int CHAN_INPUT_RGB_5		= 315;
	public static final int CHAN_INPUT_RGB_6		= 316;
	public static final int CHAN_INPUT_RGB_7		= 317;
	public static final int CHAN_INPUT_RGB_8		= 318;
	public static final int CHAN_INPUT_RGB_9		= 319;
	
	public static final int CHAN_INPUT_VIDEO_1		= 321;
	public static final int CHAN_INPUT_VIDEO_2		= 322;
	public static final int CHAN_INPUT_VIDEO_3		= 323;
	public static final int CHAN_INPUT_VIDEO_4		= 324;
	public static final int CHAN_INPUT_VIDEO_5		= 325;
	public static final int CHAN_INPUT_VIDEO_6		= 326;
	public static final int CHAN_INPUT_VIDEO_7		= 327;
	public static final int CHAN_INPUT_VIDEO_8		= 328;
	public static final int CHAN_INPUT_VIDEO_9		= 329;
	
	public static final int CHAN_INPUT_DIGITAL_1	= 331;
	public static final int CHAN_INPUT_DIGITAL_2	= 332;
	public static final int CHAN_INPUT_DIGITAL_3	= 333;
	public static final int CHAN_INPUT_DIGITAL_4	= 334;
	public static final int CHAN_INPUT_DIGITAL_5	= 335;
	public static final int CHAN_INPUT_DIGITAL_6	= 336;
	public static final int CHAN_INPUT_DIGITAL_7	= 337;
	public static final int CHAN_INPUT_DIGITAL_8	= 338;
	public static final int CHAN_INPUT_DIGITAL_9	= 339;
	
	public static final int CHAN_INPUT_STORAGE_1	= 341;
	public static final int CHAN_INPUT_STORAGE_2	= 342;
	public static final int CHAN_INPUT_STORAGE_3	= 343;
	public static final int CHAN_INPUT_STORAGE_4	= 344;
	public static final int CHAN_INPUT_STORAGE_5	= 345;
	public static final int CHAN_INPUT_STORAGE_6	= 346;
	public static final int CHAN_INPUT_STORAGE_7	= 347;
	public static final int CHAN_INPUT_STORAGE_8	= 348;
	public static final int CHAN_INPUT_STORAGE_9	= 349;
	
	public static final int CHAN_INPUT_NETWORK_1	= 351;
	public static final int CHAN_INPUT_NETWORK_2	= 352;
	public static final int CHAN_INPUT_NETWORK_3	= 353;
	public static final int CHAN_INPUT_NETWORK_4	= 354;
	public static final int CHAN_INPUT_NETWORK_5	= 355;
	public static final int CHAN_INPUT_NETWORK_6	= 356;
	public static final int CHAN_INPUT_NETWORK_7	= 357;
	public static final int CHAN_INPUT_NETWORK_8	= 358;
	public static final int CHAN_INPUT_NETWORK_9	= 359;
	
	
	// Error Status Indicators
	public static final int CHAN_ERROR_CONNECTION			= 499;
	
	public static final int CHAN_ERROR_FAN_WARNING			= 500;
	public static final int CHAN_ERROR_FAN_ERROR			= 501;
	public static final int CHAN_ERROR_LAMP_WARNING			= 502;
	public static final int CHAN_ERROR_LAMP_ERROR			= 503;
	public static final int CHAN_ERROR_TEMP_WARNING			= 504;
	public static final int CHAN_ERROR_TEMP_ERROR			= 505;
	public static final int CHAN_ERROR_COVER_WARNING		= 506;
	public static final int CHAN_ERROR_COVER_ERROR			= 507;
	public static final int CHAN_ERROR_FILTER_WARNING		= 508;
	public static final int CHAN_ERROR_FILTER_ERROR			= 509;
	public static final int CHAN_ERROR_OTHER_WARNING		= 510;
	public static final int CHAN_ERROR_OTHER_ERROR			= 511;
	public static final int CHAN_ERROR_PROJECTOR_FAILURE	= 512;
	
	/***********************************************************
 	
 	***********************************************************/
	
	private NetLinxDevice dvDuet;
	private PJLink _pjLink;

	public PJLinkModule() {
		super();
	}

	public PJLinkModule(BundleContext bctxt, NetLinxDevice nd, Properties props) {
		super(bctxt, nd, props);
		dvDuet = new NetLinxDevice(new DPS(getProperty("Duet-Device")), true);
		dvDuet.initialize();
	}

	protected void doAddNetLinxDeviceListeners() {
	}

	protected boolean doNetLinxDeviceInitialization() {
		this.getNetLinxDevice().setChannelCount(512);
		_pjLink = new PJLink();
		_pjLink.addListener(this);
		
		this.getNetLinxDevice().onFeedbackChannel(CHAN_DEVICE_IS_INITIALIZED);		// Device is online.
		this.getNetLinxDevice().onFeedbackChannel(CHAN_DATA_IS_INITIALIZED);		// Device is initialized.
		
		return true;
	}

	public boolean isDeviceOnLine() {
		return true;
	}

	public boolean isDataInitialized() {
		return true;
	}

	public void createNetLinxDevice(DPS dps) {
		super.createNetLinxDevice(dps);
	}

	public void handleAdvancedEvent(Event obj) {
		super.handleAdvancedEvent(obj);
	}

	public void handleButtonEvent(Event obj, int channel, boolean push) {
		super.handleButtonEvent(obj, channel, push);
	}

	public void handleChannelEvent(Event obj, int channel, boolean on) {
		super.handleChannelEvent(obj, channel, on);
		
		switch (channel) {
		
		// Cycle Lamp Power
		case 9:
			if (on) {
				int powerState = _pjLink.getPowerState();
				if (powerState == PJLink.POWER_ON) {
					_pjLink.powerOff();
				}
				else if (powerState == PJLink.POWER_OFF) {
					_pjLink.powerOn();
				}
			}
			break;
			
		// Lamp Power On - Momentary
		case 27:
			if (on) _pjLink.powerOn();
			break;
		
		// Lamp Power Off - Momentary
		case 28:
			if (on) _pjLink.powerOff();
			break;
			
		// Set Volume Mute
		case 199:
			if (on) {
				_pjLink.muteAudio();
			}
			else {
				_pjLink.unmuteAudio();
			}
			break;
			
		// Set Picture Mute
		// The module spec mutes BOTH audio and video when video is muted.
		case 211:
			if (on) {
				_pjLink.muteVideo();
			}
			else {
				_pjLink.unmuteVideo();
			}
			break;
			
		// Set Freeze
		case 214:
			break;
			
		// EXTENDED CHANNELS //
		
		case 311:	_pjLink.switchInput(PJLink.INPUT_RGB_1); break;
		case 312:	_pjLink.switchInput(PJLink.INPUT_RGB_2); break;
		case 313:	_pjLink.switchInput(PJLink.INPUT_RGB_3); break;
		case 314:	_pjLink.switchInput(PJLink.INPUT_RGB_4); break;
		case 315:	_pjLink.switchInput(PJLink.INPUT_RGB_5); break;
		case 316:	_pjLink.switchInput(PJLink.INPUT_RGB_6); break;
		case 317:	_pjLink.switchInput(PJLink.INPUT_RGB_7); break;
		case 318:	_pjLink.switchInput(PJLink.INPUT_RGB_8); break;
		case 319:	_pjLink.switchInput(PJLink.INPUT_RGB_9); break;
		
		case 321:	_pjLink.switchInput(PJLink.INPUT_VIDEO_1); break;
		case 322:	_pjLink.switchInput(PJLink.INPUT_VIDEO_2); break;
		case 323:	_pjLink.switchInput(PJLink.INPUT_VIDEO_3); break;
		case 324:	_pjLink.switchInput(PJLink.INPUT_VIDEO_4); break;
		case 325:	_pjLink.switchInput(PJLink.INPUT_VIDEO_5); break;
		case 326:	_pjLink.switchInput(PJLink.INPUT_VIDEO_6); break;
		case 327:	_pjLink.switchInput(PJLink.INPUT_VIDEO_7); break;
		case 328:	_pjLink.switchInput(PJLink.INPUT_VIDEO_8); break;
		case 329:	_pjLink.switchInput(PJLink.INPUT_VIDEO_9); break;
		
		case 331:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_1); break;
		case 332:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_2); break;
		case 333:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_3); break;
		case 334:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_4); break;
		case 335:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_5); break;
		case 336:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_6); break;
		case 337:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_7); break;
		case 338:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_8); break;
		case 339:	_pjLink.switchInput(PJLink.INPUT_DIGITAL_9); break;
		
		case 341:	_pjLink.switchInput(PJLink.INPUT_STORAGE_1); break;
		case 342:	_pjLink.switchInput(PJLink.INPUT_STORAGE_2); break;
		case 343:	_pjLink.switchInput(PJLink.INPUT_STORAGE_3); break;
		case 344:	_pjLink.switchInput(PJLink.INPUT_STORAGE_4); break;
		case 345:	_pjLink.switchInput(PJLink.INPUT_STORAGE_5); break;
		case 346:	_pjLink.switchInput(PJLink.INPUT_STORAGE_6); break;
		case 347:	_pjLink.switchInput(PJLink.INPUT_STORAGE_7); break;
		case 348:	_pjLink.switchInput(PJLink.INPUT_STORAGE_8); break;
		case 349:	_pjLink.switchInput(PJLink.INPUT_STORAGE_9); break;
		
		case 351:	_pjLink.switchInput(PJLink.INPUT_NETWORK_1); break;
		case 352:	_pjLink.switchInput(PJLink.INPUT_NETWORK_2); break;
		case 353:	_pjLink.switchInput(PJLink.INPUT_NETWORK_3); break;
		case 354:	_pjLink.switchInput(PJLink.INPUT_NETWORK_4); break;
		case 355:	_pjLink.switchInput(PJLink.INPUT_NETWORK_5); break;
		case 356:	_pjLink.switchInput(PJLink.INPUT_NETWORK_6); break;
		case 357:	_pjLink.switchInput(PJLink.INPUT_NETWORK_7); break;
		case 358:	_pjLink.switchInput(PJLink.INPUT_NETWORK_8); break;
		case 359:	_pjLink.switchInput(PJLink.INPUT_NETWORK_9); break;
		
		// TODO: REMOVE. This is for testing. /////////////////////////////////////////////////////////////////////////////////////////////////////////
		case 260:
			if (on) _pjLink.queryAll();
			break;
		
		default:
			break;
		}
	}

	public void handleCommandEvent(Event obj, String cmd) {
		super.handleCommandEvent(obj, cmd);
		
		int equalsPos = cmd.indexOf("=");
		
		String command = (equalsPos < 0) ? cmd : cmd.substring(0, equalsPos);
		String value = (equalsPos < 0) ? "" : cmd.substring(equalsPos + 1);
		
		// Set IP address.
		if (command.toUpperCase().equals("IPADDR")) {
			_pjLink.setIPAddress(value);
			
			if (value.length() == 0) {
				System.out.println("PJLink IP address cleared for " + dvDuet.getDPS().toString() + ".");
			}
			else {
				System.out.println("PJLink IP address set to " + _pjLink.getIPAddress() + " for " + dvDuet.getDPS().toString() + ".");
			}
		}
		
		else if (command.toUpperCase().equals("?LAMPTIME")) {
			_pjLink.queryLampHours();
		}
		
		else if (command.toUpperCase().equals("?CONN")) {
			System.out.println("PJLink " + _pjLink.getIPAddress() + " connection status: " + !_pjLink.getConnectionError());
		}
		
		else if (command.toUpperCase().equals("DEBUG")) {
			if (value.equals("1") || value.toUpperCase().equals("TRUE")) {
				_pjLink.setPrintDebug(true);
				System.out.println("Debug enabled for device " + dvDuet.getDPS().toString() + ".");
			}
			else if (value.equals("0") || value.toUpperCase().equals("FALSE")) {
				_pjLink.setPrintDebug(false);
				System.out.println("Debug disabled for device " + dvDuet.getDPS().toString() + ".");
			}
		}
		
		else if (command.toUpperCase().equals("?DEBUG")) {
			dvDuet.sendCommand("DEBUG-" + _pjLink.getPrintDebug());
		}
		
		else if (command.toUpperCase().equals("DISABLE_POLLING")) {
			if (value.equals("1") || value.toUpperCase().equals("TRUE")) {
				_pjLink.setDisablePolling(true);
				System.out.println("Polling disabled for device " + dvDuet.getDPS().toString() + ".");
			}
			else if (value.equals("0") || value.toUpperCase().equals("FALSE")) {
				_pjLink.setDisablePolling(false);
				System.out.println("Polling enabled for device " + dvDuet.getDPS().toString() + ".");
			}
		}
		
		else if (command.toUpperCase().equals("?DISABLE_POLLING")) {
			dvDuet.sendCommand("DISABLE_POLLING-" + _pjLink.getDisablePolling());
		}
		
		else if (command.toUpperCase().equals("REFRESH_INTERVAL")) {
			try {
				_pjLink.setRefreshInterval(Integer.parseInt(value));
			}
			catch (NumberFormatException ex) {
				// Don't care.
			}
		}
		
		else if (command.toUpperCase().equals("?REFRESH_INTERVAL")) {
			dvDuet.sendCommand("REFRESH_INTERVAL-" + _pjLink.getRefreshInterval());
		}
	}

	public void handleCustomEvent(Event obj, Custom cEvt) {
		super.handleCustomEvent(obj, cEvt);
	}

	public void handleDataEvent(Event obj, int type) {
		super.handleDataEvent(obj, type);
	}

	public void handleLevelEvent(Event obj, int level, Level value) {
		super.handleLevelEvent(obj, level, value);
	}

	public void handleStringEvent(Event obj, String str) {
		super.handleStringEvent(obj, str);
	}

	public void handleTimerEvent(Timer arg) {
		super.handleTimerEvent(arg);
	}

	public void passThru(byte[] buffer) {
		super.passThru(buffer);
	}

	public void deviceStateChanged(PJLink source, PJLinkEvent e) {
		switch (e.getEventType()) {
		
		case PJLinkEvent.EVENT_ERROR:
			int error = e.getEventData();
			
			if (error == 0) {
				dvDuet.offOutputChannel(CHAN_ERROR_PROJECTOR_FAILURE);
				dvDuet.offFeedbackChannel(CHAN_ERROR_PROJECTOR_FAILURE);
			}
			
			if ((error & (	PJLink.ERROR_PROJECTOR_FAILURE |
					PJLink.ERROR_COVER_ERROR | PJLink.ERROR_FAN_ERROR | PJLink.ERROR_FILTER_ERROR |
					PJLink.ERROR_LAMP_ERROR | PJLink.ERROR_OTHER_ERROR | PJLink.ERROR_TEMP_ERROR)) > 0) {
				dvDuet.onOutputChannel(CHAN_ERROR_PROJECTOR_FAILURE);
				dvDuet.onFeedbackChannel(CHAN_ERROR_PROJECTOR_FAILURE);
				System.out.println("PJLink projector failure. " + dvDuet.getDPS().toString() + " - " + source.getIPAddress()); 
			}
			
			if ((error & PJLink.ERROR_CONNECTION) > 0) {
				if (_pjLink.getConnectionError() == true) {
					dvDuet.onOutputChannel(CHAN_ERROR_CONNECTION);
					dvDuet.onFeedbackChannel(CHAN_ERROR_CONNECTION);
				}
				else {
					dvDuet.offOutputChannel(CHAN_ERROR_CONNECTION);
					dvDuet.offFeedbackChannel(CHAN_ERROR_CONNECTION);
				}
			}
			
			if ((error & PJLink.ERROR_FAN_WARNING) > 0) {
				dvDuet.onOutputChannel(CHAN_ERROR_FAN_WARNING);
				dvDuet.onFeedbackChannel(CHAN_ERROR_FAN_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_FAN_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FAN_ERROR);
			}
			else if ((error & PJLink.ERROR_FAN_ERROR) > 0) {
				dvDuet.offOutputChannel(CHAN_ERROR_FAN_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FAN_WARNING);
				dvDuet.onOutputChannel(CHAN_ERROR_FAN_ERROR);
				dvDuet.onFeedbackChannel(CHAN_ERROR_FAN_ERROR);
			}
			else {
				dvDuet.offOutputChannel(CHAN_ERROR_FAN_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FAN_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_FAN_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FAN_ERROR);
			}
			
			if ((error & PJLink.ERROR_LAMP_WARNING) > 0) {
				dvDuet.onOutputChannel(CHAN_ERROR_LAMP_WARNING);
				dvDuet.onFeedbackChannel(CHAN_ERROR_LAMP_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_LAMP_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_LAMP_ERROR);
			}
			else if ((error & PJLink.ERROR_LAMP_ERROR) > 0) {
				dvDuet.offOutputChannel(CHAN_ERROR_LAMP_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_LAMP_WARNING);
				dvDuet.onOutputChannel(CHAN_ERROR_LAMP_ERROR);
				dvDuet.onFeedbackChannel(CHAN_ERROR_LAMP_ERROR);
			}
			else {
				dvDuet.offOutputChannel(CHAN_ERROR_LAMP_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_LAMP_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_LAMP_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_LAMP_ERROR);
			}
			
			if ((error & PJLink.ERROR_TEMP_WARNING) > 0) {
				dvDuet.onOutputChannel(CHAN_ERROR_TEMP_WARNING);
				dvDuet.onFeedbackChannel(CHAN_ERROR_TEMP_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_TEMP_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_TEMP_ERROR);
			}
			else if ((error & PJLink.ERROR_TEMP_ERROR) > 0) {
				dvDuet.offOutputChannel(CHAN_ERROR_TEMP_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_TEMP_WARNING);
				dvDuet.onOutputChannel(CHAN_ERROR_TEMP_ERROR);
				dvDuet.onFeedbackChannel(CHAN_ERROR_TEMP_ERROR);
			}
			else {
				dvDuet.offOutputChannel(CHAN_ERROR_TEMP_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_TEMP_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_TEMP_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_TEMP_ERROR);
			}
			
			if ((error & PJLink.ERROR_COVER_WARNING) > 0) {
				dvDuet.onOutputChannel(CHAN_ERROR_COVER_WARNING);
				dvDuet.onFeedbackChannel(CHAN_ERROR_COVER_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_COVER_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_COVER_ERROR);
			}
			else if ((error & PJLink.ERROR_COVER_ERROR) > 0) {
				dvDuet.offOutputChannel(CHAN_ERROR_COVER_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_COVER_WARNING);
				dvDuet.onOutputChannel(CHAN_ERROR_COVER_ERROR);
				dvDuet.onFeedbackChannel(CHAN_ERROR_COVER_ERROR);
			}
			else {
				dvDuet.offOutputChannel(CHAN_ERROR_COVER_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_COVER_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_COVER_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_COVER_ERROR);
			}
			
			if ((error & PJLink.ERROR_FILTER_WARNING) > 0) {
				dvDuet.onOutputChannel(CHAN_ERROR_FILTER_WARNING);
				dvDuet.onFeedbackChannel(CHAN_ERROR_FILTER_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_FILTER_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FILTER_ERROR);
			}
			else if ((error & PJLink.ERROR_FILTER_ERROR) > 0) {
				dvDuet.offOutputChannel(CHAN_ERROR_FILTER_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FILTER_WARNING);
				dvDuet.onOutputChannel(CHAN_ERROR_FILTER_ERROR);
				dvDuet.onFeedbackChannel(CHAN_ERROR_FILTER_ERROR);
			}
			else {
				dvDuet.offOutputChannel(CHAN_ERROR_FILTER_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FILTER_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_FILTER_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_FILTER_ERROR);
			}
			
			if ((error & PJLink.ERROR_OTHER_WARNING) > 0) {
				dvDuet.onOutputChannel(CHAN_ERROR_OTHER_WARNING);
				dvDuet.onFeedbackChannel(CHAN_ERROR_OTHER_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_OTHER_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_OTHER_ERROR);
			}
			else if ((error & PJLink.ERROR_OTHER_ERROR) > 0) {
				dvDuet.offOutputChannel(CHAN_ERROR_OTHER_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_OTHER_WARNING);
				dvDuet.onOutputChannel(CHAN_ERROR_OTHER_ERROR);
				dvDuet.onFeedbackChannel(CHAN_ERROR_OTHER_ERROR);
			}
			else {
				dvDuet.offOutputChannel(CHAN_ERROR_OTHER_WARNING);
				dvDuet.offFeedbackChannel(CHAN_ERROR_OTHER_WARNING);
				dvDuet.offOutputChannel(CHAN_ERROR_OTHER_ERROR);
				dvDuet.offFeedbackChannel(CHAN_ERROR_OTHER_ERROR);
			}
			
			break;
		
		case PJLinkEvent.EVENT_POWER:
			// Lamp channel feedback.
			if (e.getEventData() == PJLink.POWER_ON || e.getEventData() == PJLink.POWER_WARMING) {
				dvDuet.onOutputChannel(CHAN_LAMP);
				dvDuet.onFeedbackChannel(CHAN_LAMP);
			}
			else {
				dvDuet.offOutputChannel(CHAN_LAMP);
				dvDuet.offFeedbackChannel(CHAN_LAMP);
			}
			
			// Warming channel feedback.
			if (e.getEventData() == PJLink.POWER_WARMING) {
				dvDuet.onOutputChannel(CHAN_WARMING);
				dvDuet.onFeedbackChannel(CHAN_WARMING);
			}
			else {
				dvDuet.offOutputChannel(CHAN_WARMING);
				dvDuet.offFeedbackChannel(CHAN_WARMING);
			}
			
			// Cooling channel feedback.
			if (e.getEventData() == PJLink.POWER_COOLING) {
				dvDuet.onOutputChannel(CHAN_COOLING);
				dvDuet.onFeedbackChannel(CHAN_COOLING);
			}
			else {
				dvDuet.offOutputChannel(CHAN_COOLING);
				dvDuet.offFeedbackChannel(CHAN_COOLING);
			}
			break;
			
		case PJLinkEvent.EVENT_INPUT:
			int active = e.getEventData();
			
			for (int i = CHAN_INPUT_RGB_1; i <= CHAN_INPUT_NETWORK_9; i++) {
				if (i == active + 300) {
					dvDuet.onOutputChannel(i);
					dvDuet.onFeedbackChannel(i);
				}
				else {
					dvDuet.offOutputChannel(i);
					dvDuet.offFeedbackChannel(i);
				}
			}
			break;
			
		case PJLinkEvent.EVENT_LAMP:
			dvDuet.sendCommand("LAMPTIME-" + e.getEventData());
			break;
			
		case PJLinkEvent.EVENT_AV_MUTE:
			switch (e.getEventData()) {
			
			case PJLink.MUTE_OFF:
				dvDuet.offOutputChannel(CHAN_AUDIO_MUTE);
				dvDuet.offFeedbackChannel(CHAN_AUDIO_MUTE);
				dvDuet.offOutputChannel(CHAN_PICTURE_MUTE);
				dvDuet.offFeedbackChannel(CHAN_PICTURE_MUTE);
				break;
				
			case PJLink.MUTE_AUDIO_VIDEO:
				dvDuet.onOutputChannel(CHAN_AUDIO_MUTE);
				dvDuet.onFeedbackChannel(CHAN_AUDIO_MUTE);
				dvDuet.onOutputChannel(CHAN_PICTURE_MUTE);
				dvDuet.onFeedbackChannel(CHAN_PICTURE_MUTE);
				break;
				
			case PJLink.MUTE_VIDEO_ONLY:
				dvDuet.offOutputChannel(CHAN_AUDIO_MUTE);
				dvDuet.offFeedbackChannel(CHAN_AUDIO_MUTE);
				dvDuet.onOutputChannel(CHAN_PICTURE_MUTE);
				dvDuet.onFeedbackChannel(CHAN_PICTURE_MUTE);
				break;
				
			case PJLink.MUTE_AUDIO_ONLY:
				dvDuet.onOutputChannel(CHAN_AUDIO_MUTE);
				dvDuet.onFeedbackChannel(CHAN_AUDIO_MUTE);
				dvDuet.offOutputChannel(CHAN_PICTURE_MUTE);
				dvDuet.offFeedbackChannel(CHAN_PICTURE_MUTE);
				break;
			
			default: break;
			}
			break;
		
		default:
			break;
		}
	}
}
