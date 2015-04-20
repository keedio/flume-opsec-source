package com.keedio.flume.source.metrics;

import javax.management.MXBean;

/**
 * Bean exposing several metrics for the opsec-source.
 */
@MXBean
public interface MetricsMBean {

    /**
     * @return Number of events correctly published to the channel.
     */
	public long getDeliveryOk();

    /**
     * @return number of events coming from FW1 processed with errors (parsing errors).
     */
	public long getProcessError();

    /**
     * @return number of messages we were not able to publish to the channel.
     */
    public long getDeliveryError();

    /**
     * @return mean rate of the number of events correctly published to the channel.
     */
    public double getDeliveryOkMeanRate();

    /**
     * @return one-minute rate of the number of events correctly published to the channel.
     */
    public double getDeliveryOkOneMinuteRate();

    /**
     * @return fifteen-minute rate of the number of events correctly published to the channel.
     */
    public double getDeliveryOkFifteenMinuteRate();

    /**
     * @return mean rate of the number of events parsed with errors.
     */
    public double getProcessErrorMeanRate();

    /**
     * @return one-minute rate of the number of events parsed with errors.
     */
    public double getProcessErrorOneMinuteRate();

    /**
     * @return fifteen-minute rate of the number of events parsed with errors.
     */
    public double getProcessErrorFifteenMinuteRate();

    /**
     * @return mean rate of the number of events we were not able to publish to the channel.
     */
    public double getDeliveryErrorMeanRate();

    /**
     * @return one-minute rate of the number of events we were not able to publish to the channel.
     */
    public double getDeliveryErrorOneMinuteRate();

    /**
     * @return fifteen-minute rate of the number of events we were not able to publish to the channel.
     */
    public double getDeliveryErrorFifteenMinuteRate();

    /**
     * @return 95th percentile of the incoming messages parse time.
     */
    public double getProcessTime95ThPercentile();

    /**
     * @return 99th percentile of the incoming messages parse time.
     */
    public double getProcessTime99thPercentile();

    /**
     * @return minimum time spent parsing incoming messages.
     */
    public double getProcessTimeMin();

    /**
     * @return maximum time spent parsing incoming messages.
     */
    public double getProcessTimeMax();

    /**
     * @return mean time spent parsing incoming messages.
     */
    public double getProcessTimeMean();

    /**
     * @return stddev of the time spent parsing incoming messages.
     */
    public double getProcessTimeStdDev();
}
