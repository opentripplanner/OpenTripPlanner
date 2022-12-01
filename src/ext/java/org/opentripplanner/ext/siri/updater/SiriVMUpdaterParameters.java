package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class SiriVMUpdaterParameters implements PollingGraphUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final boolean blockReadinessUntilInitialized;

  // Source parameters
  private final String url;
  private final String requestorRef;
  private final int frequencySec;
  private final int timeoutSec;

  public SiriVMUpdaterParameters(
    String configRef,
    String feedId,
    boolean blockReadinessUntilInitialized,
    String url,
    String requestorRef,
    int frequencySec,
    int timeoutSec
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.blockReadinessUntilInitialized = blockReadinessUntilInitialized;
    this.url = url;
    this.requestorRef = requestorRef;
    this.frequencySec = frequencySec;
    this.timeoutSec = timeoutSec;
  }

  @Override
  public int frequencySec() {
    return frequencySec;
  }

  @Override
  public String configRef() {
    return configRef;
  }

  public String getFeedId() {
    return feedId;
  }

  public boolean blockReadinessUntilInitialized() {
    return blockReadinessUntilInitialized;
  }

  public SiriVMHttpTripUpdateSource.Parameters sourceParameters() {
    return new SiriVMHttpTripUpdateSource.Parameters() {
      @Override
      public String getUrl() {
        return url;
      }

      @Override
      public String getRequestorRef() {
        return requestorRef;
      }

      @Override
      public String getFeedId() {
        return feedId;
      }

      @Override
      public int getTimeoutSec() {
        return timeoutSec;
      }
    };
  }
}
