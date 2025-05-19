package org.opentripplanner.standalone.config.routerconfig.updaters;

import org.opentripplanner.ext.siri.updater.mqtt.MqttSiriETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;

public class SiriETMqttUpdaterConfig {

  public static MqttSiriETUpdaterParameters create(String configRef, NodeAdapter siriMqttRoot) {
    String feedId = siriMqttRoot.of("feedId").since(OtpVersion.V2_8).asString();

    String url = siriMqttRoot.of("url").since(OtpVersion.V2_8).asString();

    String topic = siriMqttRoot.of("topic").since(OtpVersion.V2_8).asString();

    int qos = siriMqttRoot.of("qos").since(OtpVersion.V2_8).asInt();

    boolean fuzzyTripMatching = siriMqttRoot
      .of("fuzzyTripMatching")
      .since(OtpVersion.V2_8)
      .asBoolean();

    return new MqttSiriETUpdaterParameters(configRef, feedId, url, topic, qos, fuzzyTripMatching);
  }
}
