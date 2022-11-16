package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

public class SiriETUpdaterParameters
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {

  private final String configRef;
  private final String feedId;
  private final boolean blockReadinessUntilInitialized;

  private final String url;
  private final int frequencySec;

  // Source parameters
  private final String requestorRef;
  private final int timeoutSec;
  private final int previewIntervalMinutes;
  private final boolean fuzzyTripMatching;

  public SiriETUpdaterParameters(
    String configRef,
    String feedId,
    boolean blockReadinessUntilInitialized,
    String url,
    int frequencySec,
    String requestorRef,
    int timeoutSec,
    int previewIntervalMinutes,
    boolean fuzzyTripMatching
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.blockReadinessUntilInitialized = blockReadinessUntilInitialized;
    this.url = url;
    this.frequencySec = frequencySec;
    this.requestorRef = requestorRef;
    this.timeoutSec = timeoutSec;
    this.previewIntervalMinutes = previewIntervalMinutes;
    this.fuzzyTripMatching = fuzzyTripMatching;
  }

  public String getFeedId() {
    return feedId;
  }

  public boolean blockReadinessUntilInitialized() {
    return blockReadinessUntilInitialized;
  }

  @Override
  public int frequencySec() {
    return frequencySec;
  }

  @Override
  public String getUrl() {
    return sourceParameters().getUrl();
  }

  @Override
  public String configRef() {
    return configRef;
  }

  public boolean isFuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  public SiriETHttpTripUpdateSource.Parameters sourceParameters() {
    return new SiriETHttpTripUpdateSource.Parameters() {
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

      @Override
      public int getPreviewIntervalMinutes() {
        return previewIntervalMinutes;
      }
    };
  }
}
