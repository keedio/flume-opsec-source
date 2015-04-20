package com.keedio.flume.source;

import com.google.common.io.Files;
import com.keedio.flume.source.metrics.OpsecSourceMetrics;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.flume.*;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.ConfigurationException;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test OpsecSource
 *
 * Not portable, uses the /bin/cat unix command to test the source behaviour.
 *
 * @author  Luca Rosellini <lrosellini@keedio.com>
 */
public class OpsecSourceTest {
    private static Logger logger = Logger.getLogger("com.keedio.flume.source.OpsecSourceTest");

    private OpsecSource source;

    private ArgumentCaptor<Event> channelCaptor;
    private ChannelProcessor mockChannelProcessor;

    @Before
    public void initSource(){
        source = new OpsecSource();
        channelCaptor = ArgumentCaptor.forClass(Event.class);
        OpsecSourceMetrics opsecSourceMetrics = mock(OpsecSourceMetrics.class);
        mockChannelProcessor = mock(ChannelProcessor.class);

        source.opsecSourceMetrics = opsecSourceMetrics;
        source.fw1LogGrabberBinary = new String[]{"/bin/cat","", ""};
        source.setChannelProcessor(mockChannelProcessor);

    }

    @After
    public void cleanup(){
        if (source != null){
            source.stop();
        }
    }

    @Test
    public void testProcessLogGrabberMessage() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("fw.log")))) {
            String line = br.readLine();
            ObjectMapper mapper = new ObjectMapper();

            byte[] baJson = source.processLogGrabberMessage(line, mapper);

            Map mapJson = mapper.readValue(baJson, Map.class);

            assertNotNull(mapJson);
            assertEquals(13, mapJson.size());
            assertEquals("SALIDA\\|GENERAL\\|CSA", mapJson.get("rule_name"));
            assertEquals("<5530a3c8,000003e3,47160916,000007b6>", mapJson.get("uuid"));
        }
    }

    @Test
    public void testProcessLogGrabberMessageFieldNoEquals() throws IOException {
        String line = "time=17Apr2015  8:10:16|action=accept|localhost|i/f_dir=inbound";
        ObjectMapper mapper = new ObjectMapper();

        byte[] baJson = source.processLogGrabberMessage(line, mapper);

        Map mapJson = mapper.readValue(baJson, Map.class);

        assertNotNull(mapJson);
        assertEquals(4, mapJson.size());
        assertEquals("17Apr2015  8:10:16", mapJson.get("time"));
        assertEquals("inbound", mapJson.get("i/f_dir"));
        assertEquals("", mapJson.get("localhost"));

    }

    @Test
    public void testProcessLogGrabberEmptyMessage() throws IOException {
        String line = "";
        ObjectMapper mapper = new ObjectMapper();

        byte[] baJson = source.processLogGrabberMessage(line, mapper);

        Map mapJson = mapper.readValue(baJson, Map.class);

        logger.info(mapJson);
        assertNotNull(mapJson);
        assertEquals(0, mapJson.size());

    }

    @Test(expected = ConfigurationException.class)
    public void testSourceConfigureEmptyConfigPath(){
        Context context = mock(Context.class);
        when(context.getString("loggrabber.config.path")).thenReturn("");
        source.configure(context);
    }

    @Test(expected = ConfigurationException.class)
    public void testSourceConfigureNotExistentConfigPath(){
        Context context = mock(Context.class);

        String notExistentDir = System.getProperty("java.io.tmpdir") + File.separator + RandomStringUtils.random(10,"nfrkfhe393k43");

        when(context.getString("loggrabber.config.path")).thenReturn(notExistentDir);
        source.configure(context);
    }

    @Test(expected = ConfigurationException.class)
    public void testSourceConfigureNotExistentConfigLeaConf(){
        Context context = mock(Context.class);

        File tmpDir = Files.createTempDir();
        when(context.getString("loggrabber.config.path")).thenReturn(tmpDir.getAbsolutePath());
        source.configure(context);
    }

    @Test(expected = ConfigurationException.class)
    public void testSourceConfigureNotExistentConfigLoggrabberConf() throws IOException {
        Context context = mock(Context.class);

        File tmpDir = Files.createTempDir();
        when(context.getString("loggrabber.config.path")).thenReturn(tmpDir.getAbsolutePath());

        File leaConf = new File(tmpDir,OpsecSource.LEA_CONF_FILENAME);
        leaConf.createNewFile();

        source.configure(context);
    }

    @Test
    public void testSourceProcess() throws IOException, EventDeliveryException, InterruptedException, URISyntaxException {
        doTestProcess();

        Thread.sleep(100);

        assertEquals(PollableSource.Status.READY,source.process());
        assertEquals(PollableSource.Status.READY,source.process());
        assertEquals(PollableSource.Status.READY,source.process());
        source.stop();
        assertEquals(PollableSource.Status.BACKOFF,source.process());

        verify(mockChannelProcessor, times(3)).processEvent(channelCaptor.capture());

        List<Event> capturedEvents = channelCaptor.getAllValues();

        assertEquals(3, capturedEvents.size());

        ObjectMapper mapper = new ObjectMapper();
        Map mapJson = mapper.readValue(capturedEvents.get(0).getBody(), Map.class);

        assertEquals("17Apr2015  8:10:16", mapJson.get("time"));
        assertEquals("52905", mapJson.get("s_port"));

        mapJson = mapper.readValue(capturedEvents.get(1).getBody(), Map.class);

        assertEquals("18Apr2015  8:11:16", mapJson.get("time"));
        assertEquals("52907", mapJson.get("s_port"));

        mapJson = mapper.readValue(capturedEvents.get(2).getBody(), Map.class);

        assertEquals("16Apr2015  7:10:11", mapJson.get("time"));
        assertEquals("55850", mapJson.get("s_port"));
    }

    @Test(expected = FlumeException.class)
    public void testSourceProcessStoppedAbruptly() throws IOException, EventDeliveryException, InterruptedException, URISyntaxException {
        doTestProcess();

        Thread.sleep(100);

        source.process();
        source.process();
        source.process();
        source.process();

    }

    private void doTestProcess() throws URISyntaxException, IOException {
        URL url = this.getClass().getClassLoader().getResource("fw.log");
        File fw = new File(url.toURI());

        Context context = mock(Context.class);

        File tmpDir = Files.createTempDir();
        when(context.getString("loggrabber.config.path")).thenReturn(tmpDir.getAbsolutePath());

        File leaConf = new File(tmpDir, OpsecSource.LEA_CONF_FILENAME);
        File loggrabberConf = new File(tmpDir,OpsecSource.LOGGRABBER_CONF_FILENAME);
        leaConf.createNewFile();
        loggrabberConf.createNewFile();

        source.fw1LogGrabberBinary[1] = fw.getAbsolutePath();
        source.configure(context);
        source.start();
    }

    @Test
    public void testProcessStdErr() throws IOException {
        source.fw1LogGrabberBinary[1] = "not_existant_file";

        Context context = mock(Context.class);

        File tmpDir = Files.createTempDir();
        when(context.getString("loggrabber.config.path")).thenReturn(tmpDir.getAbsolutePath());
        File leaConf = new File(tmpDir, OpsecSource.LEA_CONF_FILENAME);
        File loggrabberConf = new File(tmpDir,OpsecSource.LOGGRABBER_CONF_FILENAME);
        leaConf.createNewFile();
        loggrabberConf.createNewFile();
        source.configure(context);

        Logger mockLogger = mock(Logger.class);
        source.setErrorLogger(mockLogger);
        source.start();

        verify(mockLogger, atLeastOnce()).error(anyString());
    }
}
