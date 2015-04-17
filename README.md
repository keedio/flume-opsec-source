Flume source that uses fw1-loggrabber to get logs from CheckPoint using the LEA API.

The Log Grabber subprocess is spawn by the flume source and it's STDOUT is piped to an in-memory queue.

The in-memory queue is then polled by Flume when the process() method is called.

This Source produces event whose body is a byte array representing an UTF-8 json string.<br/>
Example: let's assume CheckPoint sends the following message<br/>
time=17Apr2015  8:10:16|action=accept|orig=localhost|i/f_dir=inbound|i/f_name=Exp2-1.715<br/>
then this source will produce the following json:<br/>
{time:"17Apr2015  8:10:16",action:"accept",orig:"localhost",i/f_dir="inbound",i/f_name="Exp2-1.715"}

If the input line is null or empty, an empty JSON is returned.

If one of the fields of the input line is not in the form key=value, the output
json will have a field whose name is the received field and whose value is empty.
Example:

time=17Apr2015  8:10:16|action|orig=localhost

The resulting JSON will be:
{time:"17Apr2015  8:10:16",action:"accept",orig:"localhost"}

Very important: this Source assumes fw1-loggrabber produces messages whose fields are separated by the ' char.
This source correctly parses messages where the field value contains an escaped '|' char "\|".

Configuration:

the property 'loggrabber.config.path' must be configured in the flume context and assumes that this folder
contains both 'lea.conf' and 'fw1-loggrabber.conf', which are used by fw1-loggrabber to correctly nfigure its behaviour.

If the fw1 loggrabber process dies, the source tries to flush the in-memory queue to the channel ocessor before killing itself.

