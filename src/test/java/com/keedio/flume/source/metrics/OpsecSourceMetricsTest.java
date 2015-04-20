package com.keedio.flume.source.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 20/4/15.
 */
public class OpsecSourceMetricsTest {

    private OpsecSourceMetrics controller;

    @Before
    public void setup(){
        controller = new OpsecSourceMetrics();

        controller.deliveryOk = Mockito.mock(Meter.class);
        controller.processError = Mockito.mock(Meter.class);
        controller.deliveryError = Mockito.mock(Meter.class);
        controller.processTime = Mockito.mock(Histogram.class);
    }

    @Test
    public void testManageProcessOk(){
        MetricsEvent event = new MetricsEvent(MetricsEvent.PROCESS_OK);
        controller.manage(event);
        Mockito.verify(controller.deliveryOk,Mockito.times(1)).mark();
    }

    @Test
    public void testManageProcessError(){
        MetricsEvent event = new MetricsEvent(MetricsEvent.PROCESS_ERROR);
        controller.manage(event);
        Mockito.verify(controller.processError,Mockito.times(1)).mark();
    }
    @Test
    public void testManageDeliveryError(){
        MetricsEvent event = new MetricsEvent(MetricsEvent.DELIVERY_ERROR);
        controller.manage(event);
        Mockito.verify(controller.deliveryError,Mockito.times(1)).mark();
    }
    @Test
    public void testManageProcessTime(){
        ArgumentCaptor<Long> valueCaptor = ArgumentCaptor.forClass(Long.class);

        MetricsEvent event = new MetricsEvent(MetricsEvent.PROCESS_TIME, 4);
        controller.manage(event);
        Mockito.verify(controller.processTime,Mockito.times(1)).update(valueCaptor.capture());

        Assert.assertEquals(new Long(4), valueCaptor.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEventType(){
        MetricsEvent event = new MetricsEvent(7);
        controller.manage(event);

    }
}
