package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class SiriSXUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final String url;
  private final String requestorRef;
  private final int frequencySec;
  private final int earlyStartSec;
  private final int timeoutSec;
  private final boolean blockReadinessUntilInitialized;

  public SiriSXUpdaterParameters(
    String configRef,
    String feedId,
    String url,
    String requestorRef,
    int frequencySec,
    int earlyStartSec,
    int timeoutSec,
    boolean blockReadinessUntilInitialized
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.requestorRef = requestorRef;
    this.frequencySec = frequencySec;
    this.earlyStartSec = earlyStartSec;
    this.timeoutSec = timeoutSec;
    this.blockReadinessUntilInitialized = blockReadinessUntilInitialized;
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

  public String getRequestorRef() {
    return requestorRef;
  }

  public int getEarlyStartSec() {
    return earlyStartSec;
  }

  public int getTimeoutSec() {
    return timeoutSec;
  }

  public boolean blockReadinessUntilInitialized() {
    return blockReadinessUntilInitialized;
  }

  String getFeedId() {
    return feedId;
  }
}
