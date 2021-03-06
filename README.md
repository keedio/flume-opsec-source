# Flume OPSEC source

Flume source that uses the native [fw1-loggrabber](https://github.com/certego/fw1-loggrabber) utility to get logs from CheckPoint using the LEA API.

The Log Grabber subprocess is spawn by the flume source and it's STDOUT is piped to an in-memory queue.

The in-memory queue is then polled by Flume when the process() method is called.

This Source produces event whose body is a byte array representing an UTF-8 json string.<br/>
Example, let's assume CheckPoint sends the following message:

	time=17Apr2015  8:10:16|action=accept|orig=localhost|i/f_dir=inbound|i/f_name=Exp2-1.715
then this source will produce the following json:<br/>

	{time:"17Apr2015  8:10:16",action:"accept",orig:"localhost",i/f_dir="inbound",i/f_name="Exp2-1.715"}

If the input line is null or empty, an empty JSON is returned.

If one of the fields of the input line is not in the form `key=value`, the output
json will have a field whose name is the received field and whose value is empty.
Example:

	time=17Apr2015  8:10:16|action|orig=localhost

The resulting JSON will be:

	{time:"17Apr2015  8:10:16",action:"",orig:"localhost"}

**Important**: this source assumes `fw1-loggrabber` produces messages whose fields are separated by the pipe char (|).
This source correctly parses messages where the field value contains an escaped pipe char `\|`.

If the fw1 loggrabber process dies, the source tries to flush the in-memory queue to the channel processor before dying.

## Configuration

You need to configure the property `loggrabber.config.path` in the flume context to point to an existing folder containing both `lea.conf` and `fw1-loggrabber.conf` (config file names are not parametrizable).

### Example configuration

Flume context source configuration:

<pre><code>agent.sources = opsec
agent.channels = memoryChannel
agent.sinks = loggerSink

agent.sources.opsec.type = <b>com.keedio.flume.source.OpsecSource</b>
agent.sources.opsec.<b>loggrabber.config.path</b>=<b>/path/to/fw1-loggrabber/conf/dir/</b>
agent.sources.opsec.channels = memoryChannel

agent.sinks.loggerSink.type = logger
agent.sinks.loggerSink.channel = memoryChannel

agent.channels.memoryChannel.type = memory
agent.channels.memoryChannel.capacity = 10000
</code></pre>

Example LEA configuration `lea.conf`:

	## LEA Config Section
	lea_server auth_type sslca
	lea_server ip 22.15.237.33
	lea_server auth_port 18184
	opsec_sic_name "CN=my_app_name,O=IDENTIFIER..xxxxxx" 	# full OPSEC sic name as provided by opsec_pull_cert
	opsec_sslca_file /path/to/opsec.p12 	# cert file as provided by opsec_pull_cert
	lea_server opsec_entity_sic_name "CN=cp_mgmt,O=IDENTIFIER..xxxxxx"
	
Example `fw1-loggrabber.conf`:

<pre><code># DEBUG_LEVEL=&lt;debuglevel&gt;
DEBUG_LEVEL="0"					<b># DO NOT CHANGE THIS</b>

# FW1_LOGFILE=&lt;Name of FW1-Logfilename&gt;
FW1_LOGFILE="fw.log"

# FW1_OUTPUT=&lt;files|logs&gt;
FW1_OUTPUT="logs"

# FW1_TYPE=&lt;ng|2000&gt;
FW1_TYPE="ng"

# FW1_MODE=&lt;audit|normal&gt;
FW1_MODE="normal"

# ONLINE_MODE=&lt;yes|no&gt;
ONLINE_MODE="yes"
	
# RESOLVE_MODE=&lt;yes|no&gt;
RESOLVE_MODE="yes"

# RECORD_SEPARATOR=&lt;char&gt;
RECORD_SEPARATOR="|"			<b># DO NOT CHANGE THIS</b>

# LOGGING_CONFIGURATION=&lt;screen|file|syslog&gt;
LOGGING_CONFIGURATION=screen	<b># DO NOT CHANGE THIS</b>
</code></pre>

**Important:** it's mandatory to set `LOGGING_CONFIGURATION=screen` and `DEBUG_LEVEL="0"` since the OPSEC source will parse log messages from STDOUT.


