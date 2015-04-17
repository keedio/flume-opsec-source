package com.keedio.flume.source.metrics;

/**
 * 
 * Definition of event types used for management by the controller metric metric.
 *
 */
public class MetricsEvent {
	
	public static final int PROCESS_OK = 1;
	public static final int PROCESS_TIME = 2;
    public static final int PROCESS_ERROR = 3;
	public static final int DELIVERY_ERROR = 4;
	
	private int code;
	private long value = -1;
	
	public MetricsEvent(int code) {
		this.code = code;
	}
	
	public MetricsEvent(int code, long value) {
		this.code = code;
		this.value = value;
	}

	public int getCode() {
		return code;
	}
	
	public long getValue() {
		return this.value;
	}

}
