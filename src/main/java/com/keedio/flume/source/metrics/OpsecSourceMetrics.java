package com.keedio.flume.source.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.apache.flume.instrumentation.MonitoredCounterGroup;

/**
 * This class represents the controller metrics to publish to the source.
 * Extends MonitoredCounterGroup class to allow the publication of JMX metrics
 * following the mechanism established by Flume.
 */
public class OpsecSourceMetrics extends MonitoredCounterGroup implements MetricsMBean {

    Meter deliveryOk;
    Meter processError;
    Meter deliveryError;
    Histogram processTime;

    private MetricRegistry metrics;

    private static final String[] ATTRIBUTES = {
            "source.meter.deliveryOk.count",
            "source.meter.processError.count",
            "source.meter.deliveryError.count",

            "source.meter.deliveryOk.meanRate",
            "source.meter.processError.meanRate",
            "source.meter.deliveryError.meanRate",

            "source.meter.deliveryOk.OneMinuteRate",
            "source.meter.processError.OneMinuteRate",
            "source.meter.deliveryError.OneMinuteRate",

            "source.meter.deliveryOk.FifteenMinuteRate",
            "source.meter.processError.FifteenMinuteRate",
            "source.meter.deliveryError.FifteenMinuteRate",

            "source.mean.process.time", "source.total.file.events"};

    /**
     * Default constructor.
     *
     */
    public OpsecSourceMetrics() {
        super(Type.SOURCE, OpsecSourceMetrics.class.getName(), ATTRIBUTES);

        metrics = new MetricRegistry();
        deliveryOk = metrics.meter("deliveryOk");
        processError = metrics.meter("processError");
        deliveryError = metrics.meter("deliveryError");
        processTime = metrics.histogram("processTime");
    }

    /**
     * This method manages metric based on events received.
     * <p/>
     * For new metrics will need to create the corresponding event type in
     * MetricsEvent class and then define their behavior here
     *
     * @param event event to manage
     * @return
     * @see
     */
    public void manage(MetricsEvent event) {

        switch (event.getCode()) {
            case MetricsEvent.PROCESS_OK:
                deliveryOk.mark();
                break;
            case MetricsEvent.PROCESS_ERROR:
                processError.mark();
                break;
            case MetricsEvent.DELIVERY_ERROR:
                deliveryError.mark();
                break;

            case MetricsEvent.PROCESS_TIME:
                processTime.update(event.getValue());
                break;
            default:
                throw new IllegalArgumentException("Event type not recognized");
        }
    }

    @Override
    public long getDeliveryOk() {
        return deliveryOk.getCount();
    }

    @Override
    public long getProcessError() {
        return processError.getCount();
    }

    @Override
    public long getDeliveryError() {
        return deliveryError.getCount();
    }

    @Override
    public double getDeliveryOkMeanRate() {
        return deliveryOk.getMeanRate();
    }

    @Override
    public double getDeliveryOkOneMinuteRate() {
        return deliveryOk.getOneMinuteRate();
    }

    @Override
    public double getDeliveryOkFifteenMinuteRate() {
        return deliveryOk.getFifteenMinuteRate();
    }

    @Override
    public double getProcessErrorMeanRate() {
        return processError.getMeanRate();
    }

    @Override
    public double getProcessErrorOneMinuteRate() {
        return processError.getOneMinuteRate();
    }

    @Override
    public double getProcessErrorFifteenMinuteRate() {
        return processError.getFifteenMinuteRate();
    }

    @Override
    public double getDeliveryErrorMeanRate() {
        return deliveryError.getMeanRate();
    }

    @Override
    public double getDeliveryErrorOneMinuteRate() {
        return deliveryError.getOneMinuteRate();
    }

    @Override
    public double getDeliveryErrorFifteenMinuteRate() {
        return deliveryError.getFifteenMinuteRate();
    }

    @Override
    public double getProcessTime95ThPercentile() {
        return processTime.getSnapshot().get95thPercentile();
    }

    @Override
    public double getProcessTime99thPercentile() {
        return processTime.getSnapshot().get99thPercentile();
    }

    @Override
    public double getProcessTimeMin() {
        return processTime.getSnapshot().getMin();
    }

    @Override
    public double getProcessTimeMax() {
        return processTime.getSnapshot().getMax();
    }

    @Override
    public double getProcessTimeMean() {
        return processTime.getSnapshot().getMean();
    }

    @Override
    public double getProcessTimeStdDev() {
        return processTime.getSnapshot().getStdDev();
    }

}
