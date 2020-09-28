package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdater;

public class MqttGtfsRealtimeUpdaterConfig implements MqttGtfsRealtimeUpdater.Parameters {
  private final String url;
  private final String topic;
  private final String feedId;
  private final int qos;
  private final boolean fuzzyTripMatching;

  public MqttGtfsRealtimeUpdaterConfig(NodeAdapter c) {
    url = c.asText("url");
    topic = c.asText("topic");
    feedId = c.asText("feedId", null);
    qos = c.asInt("qos", 0);
    fuzzyTripMatching = c.asBoolean("fuzzyTripMatching", false);
  }

  public String getUrl() {
    return url;
  }

  public String getTopic() {
    return topic;
  }

  public String getFeedId() {
    return feedId;
  }

  public int getQos() {
    return qos;
  }

  public boolean getFuzzyTripMatching() {
    return fuzzyTripMatching;
  }
}
