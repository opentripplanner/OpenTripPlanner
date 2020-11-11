package org.opentripplanner.updater.stoptime;

public class WebsocketGtfsRealtimeUpdaterParameters {
  private final String configRef;
  private final String feedId;
  private final String url;
  private final int reconnectPeriodSec;

  public WebsocketGtfsRealtimeUpdaterParameters(
      String configRef, String feedId, String url, int reconnectPeriodSec
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.reconnectPeriodSec = reconnectPeriodSec;
  }

  String getUrl() {
    return url;
  }

  String getFeedId() {
    return feedId;
  }

  int getReconnectPeriodSec() {
    return reconnectPeriodSec;
  }

  /** The config name/type for the updater. Used to reference the configuration element. */
  String getConfigRef() {
    return configRef;
  }
}
