package org.opentripplanner.standalone.config.routerconfig.updaters;

import java.time.Duration;
import org.opentripplanner.ext.siri.updater.mqtt.MqttSiriETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class SiriETMqttUpdaterConfig {

  public static MqttSiriETUpdaterParameters create(String configRef, NodeAdapter siriMqttRoot) {
    String feedId = siriMqttRoot
      .of("feedId")
      .since(OtpVersion.V2_9)
      .summary("The feed ID this updater should be applied to")
      .asString();

    String host = siriMqttRoot
      .of("host")
      .since(OtpVersion.V2_9)
      .summary("The host of the MQTT broker")
      .asString();
    int port = siriMqttRoot
      .of("port")
      .since(OtpVersion.V2_9)
      .summary("The port of the MQTT broker")
      .asInt();
    String user = siriMqttRoot
      .of("user")
      .since(OtpVersion.V2_9)
      .summary("The user for authorization at the MQTT broker")
      .description("If no authorization is required, the user does not need to be supplied.")
      .asString(null);
    String password = siriMqttRoot
      .of("password")
      .since(OtpVersion.V2_9)
      .summary("The password for authorization at the MQTT broker")
      .description("If no authorization is required, the password does not need to be supplied.")
      .asString(null);

    String topic = siriMqttRoot
      .of("topic")
      .since(OtpVersion.V2_9)
      .summary("The topic the updater should subscribe to")
      .asString();

    int qos = siriMqttRoot
      .of("qos")
      .since(OtpVersion.V2_9)
      .summary("The qos used for the MQTT subscription")
      .asInt();

    boolean fuzzyTripMatching = siriMqttRoot
      .of("fuzzyTripMatching")
      .summary("Whether or not the fuzzy trip matcher should be used")
      .since(OtpVersion.V2_9)
      .asBoolean();

    int numberOfPrimingWorkers = siriMqttRoot
      .of("numberOfPrimingWorkers")
      .since(OtpVersion.V2_9)
      .summary("Number of priming workers to process retained messages")
      .description(
        """
        The updater will only be primed and therefore ready for routing requests, when all
        retained messages are processed. If the number of retained messages is large, this could
        take a considerable amount of time. Profiling shows that the bottleneck is in the XML
        parsing. Increasing the number of priming workers will parallelize parsing. Tests show
        that 4 priming workers seem to be the optimal amount, increasing the number of priming
        workers further will decrease performance. The reception of the messages and the updating
        of the graph always occurs in a single thread.
        """
      )
      .asInt(1);

    Duration maxPrimingIdleTime = siriMqttRoot
      .of("maxPrimingIdleTime")
      .since(OtpVersion.V2_9)
      .summary("Max idle time until priming is considered complete.")
      .description(
        """
        The worker(s) processing the retained messages for priming shut down when they are idle for
        the specified amount of time. When all priming workers are shut down, the updater is
        considered to be primed.
        """
      )
      .asDuration(Duration.ofSeconds(1));

    return new MqttSiriETUpdaterParameters(
      configRef,
      feedId,
      host,
      port,
      user,
      password,
      topic,
      qos,
      fuzzyTripMatching,
      numberOfPrimingWorkers,
      maxPrimingIdleTime
    );
  }
}
