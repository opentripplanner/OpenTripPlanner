# Logging

OTP uses [logback](http://logback.qos.ch/) and [slj4j](http://www.slf4j.org/) as a logging framework.
Logging is configured in the `logback.xml` file inside the OTP jar file. See these frameworks for
more documentation on log configuration.

For developers, starting OTP using the `InteractiveOtpMain` is an easy way to configure debug
logging.

Some loggers useful for debugging.

- `TRANSFERS_EXPORT`: Dump transfers to _transfers-debug.csv_ file.
- `DATA_IMPORT_ISSUES`: Write issues to debug lag as well as to the issue report.
- `org.opentripplanner.raptor.RaptorService`: Debug Raptor request and response

## Format

By default, OTP logs in plain text to the console. However, it is possible to also log in JSON format.

To enable it, set the Java property `otp.logging.format` to one of these values:

- `plain`: regular plain text logging (default, no need to configure it)
- `json`: Logstash-encoded JSON format understood by many log ingestion tools (Datadog, Loggly, Loki...)

### Complete example

```
java -Dotp.logging.format=json -jar otp.jar --load --serve data
```

### Further customization

If you want to customize the exact log output even further you can use your own logback configuration 
by starting OTP with the following parameter:

```
java -Dlogback.configurationFile=/path/to/logback.xml -jar otp.jar --load --serve data
```

For example, Entur has their own custom log format configured as follows:

```xml
<!-- Entur's custom log format  -->
<appender name="entur" class="ch.qos.logback.core.ConsoleAppender">
  <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
     <providers>
        <!-- provides the timestamp <timestamp/> -->
        <!-- provides the version <version/> -->
        <!-- provides the fields in the configured pattern -->
        <pattern>
           <!-- the pattern that defines what to include -->
           <pattern>
           {
             "serviceContext": {
               "service": "otp2"
             },
             "message": "%message\n%ex{full}",
             "severity": "%level",
             "reportLocation": {
               "filePath": "%logger",
               "lineNumber": "%line",
               "functionName": "%method"
              }
            }
            </pattern>
        </pattern>
     </providers>
  </encoder>
</appender>
```