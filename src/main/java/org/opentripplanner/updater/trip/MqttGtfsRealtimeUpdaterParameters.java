package org.opentripplanner.updater.trip;

public class MqttGtfsRealtimeUpdaterParameters implements UrlUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final String url;
  private final String topic;
  private final int qos;
  private final boolean fuzzyTripMatching;
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  public MqttGtfsRealtimeUpdaterParameters(
    String configRef,
    String feedId,
    String url,
    String topic,
    int qos,
    boolean fuzzyTripMatching,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.topic = topic;
    this.qos = qos;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.backwardsDelayPropagationType = backwardsDelayPropagationType;
  }

  public String getUrl() {
    return url;
  }

  String getTopic() {
    return topic;
  }

  public String getFeedId() {
    return feedId;
  }

  int getQos() {
    return qos;
  }

  boolean getFuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  BackwardsDelayPropagationType getBackwardsDelayPropagationType() {
    return backwardsDelayPropagationType;
  }

  /** The config name/type for the updater. Used to reference the configuration element. */
  public String configRef() {
    return configRef;
  }
}
