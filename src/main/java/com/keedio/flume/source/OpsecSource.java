package com.keedio.flume.source;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.keedio.flume.source.metrics.MetricsController;
import com.keedio.flume.source.metrics.MetricsEvent;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.conf.ConfigurationException;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.source.AbstractSource;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * <p>
 * Flume source that uses fw1-loggrabber to get logs from CheckPoint using the LEA API.
 * </p>
 *
 * <p>
 * The Log Grabber subprocess is spawn by the flume source and it's STDOUT is piped to an in-memory queue.
 * </p>
 * <p>
 * The in-memory queue is then polled by Flume when the process() method is called.
 * </p>
 * <p>
 *     This Source produces event whose body is a byte array representing an UTF-8 json string.<br/>
 *     Example: let's assume CheckPoint sends the following message<br/>
 *     time=17Apr2015  8:10:16|action=accept|orig=localhost|i/f_dir=inbound|i/f_name=Exp2-1.715<br/>
 *     then this source will produce the following json:<br/>
 *     {time:"17Apr2015  8:10:16",action:"accept",orig:"localhost",i/f_dir="inbound",i/f_name="Exp2-1.715"}<br/>
 * </p>
 * <p>
 *     Very important: this Source assumes fw1-loggrabber produces messages whose fields are separated by the '|' char.
 *     This source correctly parses messages where the field value contains an escaped '|' char "\|".
 * </p>
 * <p>
 *     <b>Configuration:</b><br/>
 *     the property 'loggrabber.config.path' must be configured in the flume context and assumes that this folder
 *     contains both 'lea.conf' and 'fw1-loggrabber.conf', which are used by fw1-loggrabber to correctly configure its behaviour.
 * </p>
 * <p>
 *     If the fw1 loggrabber process dies, the source tries to flush the in-memory queue to the channel processor before killing itself.
 * </p>
 *
 * @author  Luca Rosellini <lrosellini@keedio.com>
 */
public class OpsecSource extends AbstractSource implements Configurable, PollableSource {
    private static Logger logger = Logger.getLogger("com.keedio.flume.source.OpsecSource");

    String[] fw1LogGrabberBinary = new String[]{"/usr/bin/fw1-loggrabber"};

    private static final String LOGGRABBER_TEMP_PATH = "LOGGRABBER_TEMP_PATH";
    private static final String LOGGRABBER_CONFIG_PATH = "LOGGRABBER_CONFIG_PATH";

    static final String LEA_CONF_FILENAME = "lea.conf";
    static final String LOGGRABBER_CONF_FILENAME = "fw1-loggrabber.conf";

    private final Queue<String> queue = new ConcurrentLinkedQueue<String>();
    private static ObjectMapper mapper = new ObjectMapper();

    private final static Pattern LOG_SPLIT_PATTERN = Pattern.compile("(?<!\\\\)\\|");

    MetricsController metricsController = new MetricsController();

    private ProcessBuilder processBuilder;
    private Process logGrabberProcess;

    /**
     * We need only the following property:
     * loggrabber.config.path
     *
     * @param context
     */
    @Override
    public void configure(Context context) {

        String logGrabberConfPathProp = context.getString("loggrabber.config.path");

        if (StringUtils.isEmpty(logGrabberConfPathProp)){
            throw new ConfigurationException("loggrabber.config.path property cannot be empty");
        }

        File logGrabberConfPath = new File(logGrabberConfPathProp);
        if (!logGrabberConfPath.exists()){
            throw new ConfigurationException(String.format("directory '%s' must exist", logGrabberConfPathProp));
        }

        File leaConf = new File(logGrabberConfPath, LEA_CONF_FILENAME);
        if (!leaConf.exists()){
            throw new ConfigurationException(String.format("file '%s' must exist", leaConf.getAbsolutePath()));
        }

        File logGrabberConf = new File(logGrabberConfPath, LOGGRABBER_CONF_FILENAME);
        if (!logGrabberConf.exists()){
            throw new ConfigurationException(String.format("file '%s' must exist", logGrabberConf.getAbsolutePath()));
        }

        File myTempDir = Files.createTempDir();

        logger.info(String.format("Using '%s' as loggrabber temp directory", myTempDir.getAbsolutePath()));

        processBuilder = new ProcessBuilder().command(fw1LogGrabberBinary);

        Map<String, String> env = processBuilder.environment();
        env.put(LOGGRABBER_TEMP_PATH, myTempDir.getAbsolutePath());
        env.put(LOGGRABBER_CONFIG_PATH, logGrabberConfPathProp);


    }

    @Override
    public Status process() throws EventDeliveryException {
        String line = queue.poll();

        if (StringUtils.isNotEmpty(line)){
            Event event = new SimpleEvent();

            try {
                event.setBody(processLogGrabberMessage(line, mapper));
            } catch (IOException e) {
                metricsController.manage(new MetricsEvent(MetricsEvent.PROCESS_ERROR));
                logger.error("Cannot process message: "+line+"\n", e);

                throw new EventDeliveryException(e);
            }

            try {
                getChannelProcessor().processEvent(event);
                metricsController.manage(new MetricsEvent(MetricsEvent.PROCESS_OK));

                return Status.READY;
            }catch (ChannelException e) {
                metricsController.manage(new MetricsEvent(MetricsEvent.DELIVERY_ERROR));
                logger.error("Cannot process message: "+line+"\n", e);

                throw e;
            }
        } else {
            checkProcessTerminated();
        }
        return Status.BACKOFF;
    }

    /**
     * Parses the input line obtained from the embedded process to a JSON byte array.
     *
     * <p>
     *     Assumes the input line is in the form:
     *     time=17Apr2015  8:10:16|action=accept|orig=localhost
     *
     *     I.E, a collection of key=value separated by pipes.
     *
     * </p>
     * <p>
     *     If the input line is null or empty, an empty JSON is returned.
     * </p>
     * <p>
     *     If one of the fields of the input line is not in the form key=value, the output
     *     json will have a field whose name is the received field and whose value is empty.
     *     Example:
     *     time=17Apr2015  8:10:16|action|orig=localhost
     *
     *     The resulting JSON will be:
     *     {time:"17Apr2015  8:10:16",action:"accept",orig:"localhost"}
     * </p>
     *
     * @param line the input line to convert.
     * @param mapper the jackson object mapper.
     * @return
     * @throws IOException
     */
    byte[] processLogGrabberMessage(String line, ObjectMapper mapper) throws IOException {
        long t0 = System.currentTimeMillis();

        try {
            if (StringUtils.isEmpty(line)) {
                return "{}".getBytes();
            }

            // split on each "|" that is not preceded by a backslash
            Iterable<String> entries = Splitter.on(LOG_SPLIT_PATTERN).split(line);

            Map<String, String> fields = new TreeMap<>();

            for (String entry : entries) {
                String[] keyValue = entry.split("=");

                if (keyValue.length > 1)
                    fields.put(keyValue[0], keyValue[1]);
                else
                    fields.put(keyValue[0], "");
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                mapper.writeValue(baos, fields);
                return baos.toByteArray();
            }
        } finally {
            long t1 = System.currentTimeMillis();

            metricsController.manage(new MetricsEvent(MetricsEvent.PROCESS_TIME, (t1-t0)));
        }
    }

    /**
     * Starts the source, the embedded fw1LogGrabber process and the StreamGobbler.
     */
    @Override
    public synchronized void start() {
        super.start();

        try {
            logGrabberProcess = processBuilder.start();
        } catch (IOException e) {
            throw new ConfigurationException("Cannot start embedded "+ fw1LogGrabberBinary,e);
        }

        metricsController.start();
        StreamGobbler outputGobbler = new StreamGobbler(logGrabberProcess.getInputStream(), "OUTPUT");
        outputGobbler.run();
    }

    /**
     * Sends a kill signal to fw1LogGrabber before terminating this source.
     */
    @Override
    public synchronized void stop() {
        super.stop();

        if (logGrabberProcess == null){
            return;
        }

        logGrabberProcess.destroy();

        try {
            logGrabberProcess.waitFor();
        } catch (InterruptedException e) {
            logger.error(e);
        }
    }

    /**
     * Checks if the embedded process has died. If the embedded process dies and the source is not
     * stopping, throws an exception
     *
     * @throws java.lang.IllegalStateException
     */
    private void checkProcessTerminated(){
        try {
            int exitValue = logGrabberProcess.exitValue();

            if (getLifecycleState() != LifecycleState.STOP)
                throw new IllegalStateException("fw1LogGrabber has terminated with exit status: " + exitValue);
        } catch (IllegalThreadStateException e) {
            // ok, do nothing
        }
    }

    /**
     * Retrieves the stream of chars from the embedded process standard output and pushes it
     * to the in-memory queue.
     */
    class StreamGobbler extends Thread {
        InputStream is;
        String type;

        private StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    queue.offer(line);
                }
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
