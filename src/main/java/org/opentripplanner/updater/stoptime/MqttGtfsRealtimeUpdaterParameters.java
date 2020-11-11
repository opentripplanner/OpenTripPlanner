package org.opentripplanner.updater.stoptime;

public class MqttGtfsRealtimeUpdaterParameters {
  private final String configRef;
  private final String feedId;
  private final String url;
  private final String topic;
  private final int qos;
  private final boolean fuzzyTripMatching;


  public MqttGtfsRealtimeUpdaterParameters(
      String configRef, String feedId, String url, String topic, int qos, boolean fuzzyTripMatching
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.topic = topic;
    this.qos = qos;
    this.fuzzyTripMatching = fuzzyTripMatching;
  }

  String getUrl() {
    return url;
  }

  String getTopic() {
    return topic;
  }

  String getFeedId() {
    return feedId;
  }

  int getQos() {
    return qos;
  }

  boolean getFuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  /** The config name/type for the updater. Used to reference the configuration element. */
  String getConfigRef() {
    return configRef;
  }
}
