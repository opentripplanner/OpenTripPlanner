package org.opentripplanner.ext.siri.updater.mqtt;

import org.opentripplanner.updater.trip.UrlUpdaterParameters;

public class MqttSiriETUpdaterParameters implements UrlUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final String url;
  private final String topic;
  private final int qos;
  private final boolean fuzzyTripMatching;

  public MqttSiriETUpdaterParameters(
    String configRef,
    String feedId,
    String url,
    String topic,
    int qos,
    boolean fuzzyTripMatching
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.topic = topic;
    this.qos = qos;
    this.fuzzyTripMatching = fuzzyTripMatching;
  }

  @Override
  public String url() {
    return url;
  }

  @Override
  public String configRef() {
    return configRef;
  }

  @Override
  public String feedId() {
    return feedId;
  }

  public String topic() {
    return topic;
  }

  public int qos() {
    return qos;
  }

  public boolean fuzzyTripMatching() {
    return fuzzyTripMatching;
  }
}
