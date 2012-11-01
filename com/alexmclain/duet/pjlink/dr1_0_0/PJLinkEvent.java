package com.alexmclain.duet.pjlink.dr1_0_0;

public class PJLinkEvent {
	
	public static final int EVENT_ERROR			= 0;
	public static final int EVENT_POWER			= 1;
	public static final int EVENT_INPUT			= 2;
	public static final int EVENT_AV_MUTE		= 3;
	public static final int EVENT_LAMP			= 4;
	
	private int _eventType;
	private int _data;
	private String _message = "";
	
	public PJLinkEvent(int eventType, int data) {
		_eventType = eventType;
		_data = data;
	}
	
	public PJLinkEvent(int eventType, int data, String message) {
		_eventType = eventType;
		_data = data;
		_message = message;
	}
	
	public int getEventType() {
		return _eventType;
	}
	
	public int getEventData() {
		return _data;
	}
	
	public String getEventMessage() {
		return _message;
	}
}
