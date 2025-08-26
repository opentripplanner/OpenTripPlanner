package org.opentripplanner.standalone.config.routerconfig.updaters;

import org.opentripplanner.ext.siri.updater.mqtt.MqttSiriETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class SiriETMqttUpdaterConfig {

  public static MqttSiriETUpdaterParameters create(String configRef, NodeAdapter siriMqttRoot) {
    String feedId = siriMqttRoot.of("feedId").since(OtpVersion.V2_8).asString();

    String host = siriMqttRoot.of("host").since(OtpVersion.V2_8).asString();
    int port = siriMqttRoot.of("port").since(OtpVersion.V2_8).asInt();
    String user = siriMqttRoot.of("user").since(OtpVersion.V2_8).asString(null);
    String password = siriMqttRoot.of("password").since(OtpVersion.V2_8).asString(null);

    String topic = siriMqttRoot.of("topic").since(OtpVersion.V2_8).asString();

    int qos = siriMqttRoot.of("qos").since(OtpVersion.V2_8).asInt();

    boolean fuzzyTripMatching = siriMqttRoot
      .of("fuzzyTripMatching")
      .since(OtpVersion.V2_8)
      .asBoolean();

    return new MqttSiriETUpdaterParameters(
      configRef,
      feedId,
      host,
      port,
      user,
      password,
      topic,
      qos,
      fuzzyTripMatching
    );
  }
}
