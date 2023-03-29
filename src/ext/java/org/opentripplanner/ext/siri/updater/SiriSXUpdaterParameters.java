package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public class SiriSXUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final String url;
  private final String requestorRef;
  private final int frequencySec;
  private final int earlyStartSec;
  private final int timeoutSec;
  private final boolean blockReadinessUntilInitialized;

  private final HttpHeaders requestHeaders;

  public SiriSXUpdaterParameters(
    String configRef,
    String feedId,
    String url,
    String requestorRef,
    int frequencySec,
    int earlyStartSec,
    int timeoutSec,
    boolean blockReadinessUntilInitialized,
    HttpHeaders requestHeaders
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.url = url;
    this.requestorRef = requestorRef;
    this.frequencySec = frequencySec;
    this.earlyStartSec = earlyStartSec;
    this.timeoutSec = timeoutSec;
    this.blockReadinessUntilInitialized = blockReadinessUntilInitialized;
    this.requestHeaders = requestHeaders;
  }

  String getFeedId() {
    return feedId;
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

  public HttpHeaders requestHeaders() {
    return requestHeaders;
  }
}
