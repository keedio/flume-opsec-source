package com.keedio.flume.source.metrics;

import javax.management.MXBean;

@MXBean
public interface MetricsMBean {

	
	public long getDeliveryOk();
	public long getProcessError();
    public long getDeliveryError();

    public double getDeliveryOkMeanRate();
    public double getDeliveryOkOneMinuteRate();
    public double getDeliveryOkFifteenMinuteRate();

    public double getProcessErrorMeanRate();
    public double getProcessErrorOneMinuteRate();
    public double getProcessErrorFifteenMinuteRate();

    public double getDeliveryErrorMeanRate();
    public double getDeliveryErrorOneMinuteRate();
    public double getDeliveryErrorFifteenMinuteRate();

    public double getProcessTime95ThPercentile();
    public double getProcessTime99thPercentile();
    public double getProcessTimeMin();
    public double getProcessTimeMax();
    public double getProcessTimeMean();
    public double getProcessTimeStdDev();
}
