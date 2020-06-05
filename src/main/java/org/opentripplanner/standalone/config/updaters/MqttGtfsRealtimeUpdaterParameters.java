package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdater;

public class MqttGtfsRealtimeUpdaterParameters implements MqttGtfsRealtimeUpdater.Parameters {
  private final String url;
  private final String topic;
  private final String feedId;
  private final int qos;

  public MqttGtfsRealtimeUpdaterParameters(NodeAdapter c) {
    url = c.asText("url");
    topic = c.asText("topic");
    feedId = c.asText("feedId", null);
    qos = c.asInt("qos", 0);
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
}
