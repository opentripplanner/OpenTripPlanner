package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdater;

public class WebsocketGtfsRealtimeUpdaterConfig
    implements WebsocketGtfsRealtimeUpdater.Parameters {
  private final String configRef;
  private final String url;
  private final String feedId;
  private final int reconnectPeriodSec;

  public WebsocketGtfsRealtimeUpdaterConfig(String configRef, NodeAdapter c) {
    this.configRef = configRef;
    this.url = c.asText("url", null);
    this.reconnectPeriodSec = c.asInt("reconnectPeriodSec", 60);
    this.feedId = c.asText("feedId", null);
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getFeedId() {
    return feedId;
  }

  @Override
  public int getReconnectPeriodSec() {
    return reconnectPeriodSec;
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }
}