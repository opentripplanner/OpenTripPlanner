package org.opentripplanner.updater.trip;

public class WebsocketGtfsRealtimeUpdaterParameters implements UrlUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final String url;
  private final int reconnectPeriodSec;
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  public WebsocketGtfsRealtimeUpdaterParameters(
    String configRef,
    String feedId,
    String url,
    int reconnectPeriodSec,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.reconnectPeriodSec = reconnectPeriodSec;
    this.backwardsDelayPropagationType = backwardsDelayPropagationType;
  }

  public String url() {
    return url;
  }

  public String feedId() {
    return feedId;
  }

  int getReconnectPeriodSec() {
    return reconnectPeriodSec;
  }

  /** The config name/type for the updater. Used to reference the configuration element. */
  public String configRef() {
    return configRef;
  }

  public BackwardsDelayPropagationType getBackwardsDelayPropagationType() {
    return backwardsDelayPropagationType;
  }
}
