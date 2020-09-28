package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdater;

public class MqttGtfsRealtimeUpdaterConfig implements MqttGtfsRealtimeUpdater.Parameters {
  private final String configRef;
  private final String url;
  private final String topic;
  private final String feedId;
  private final int qos;
  private final boolean fuzzyTripMatching;


  public MqttGtfsRealtimeUpdaterConfig(String configRef, NodeAdapter c) {
    this.configRef = configRef;
    this.url = c.asText("url");
    this.topic = c.asText("topic");
    this.feedId = c.asText("feedId", null);
    this.qos = c.asInt("qos", 0);
    this.fuzzyTripMatching = c.asBoolean("fuzzyTripMatching", false);
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getTopic() {
    return topic;
  }

  @Override
  public String getFeedId() {
    return feedId;
  }

  @Override
  public int getQos() {
    return qos;
  }

  @Override
  public boolean getFuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }
}
