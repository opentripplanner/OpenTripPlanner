package org.opentripplanner.updater.alert;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class GtfsRealtimeAlertsUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final String url;
  private final int earlyStartSec;
  private final boolean fuzzyTripMatching;
  private final int frequencySec;

  public GtfsRealtimeAlertsUpdaterParameters(
    String configRef,
    String feedId,
    String url,
    int earlyStartSec,
    boolean fuzzyTripMatching,
    int getFrequencySec
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.earlyStartSec = earlyStartSec;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.frequencySec = getFrequencySec;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public int frequencySec() {
    return frequencySec;
  }

  @Override
  public String configRef() {
    return configRef;
  }

  String getFeedId() {
    return feedId;
  }

  int getEarlyStartSec() {
    return earlyStartSec;
  }

  boolean fuzzyTripMatching() {
    return fuzzyTripMatching;
  }
}
