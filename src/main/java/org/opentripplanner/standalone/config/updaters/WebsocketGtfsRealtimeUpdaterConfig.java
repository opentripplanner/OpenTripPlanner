package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;

public class WebsocketGtfsRealtimeUpdaterConfig
    implements WebsocketGtfsRealtimeUpdater.Parameters {
  private final String url;
  private final String feedId;
  private final int reconnectPeriodSec;

  public WebsocketGtfsRealtimeUpdaterConfig(NodeAdapter c) {
    url = c.asText("url", null);
    reconnectPeriodSec = c.asInt("reconnectPeriodSec", 60);
    feedId = c.asText("feedId", null);
  }

  public String getUrl() {
    return url;
  }
  public String getFeedId() {
    return feedId;
  }
  public int getReconnectPeriodSec() {
    return reconnectPeriodSec;
  }
}