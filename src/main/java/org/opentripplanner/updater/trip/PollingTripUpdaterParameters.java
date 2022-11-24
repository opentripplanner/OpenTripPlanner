package org.opentripplanner.updater.trip;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class PollingTripUpdaterParameters
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {

  private final String configRef;
  private final int frequencySec;
  private final boolean fuzzyTripMatching;
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  private final String feedId;
  private final String httpSourceUrl;
  private final String fileSource;

  public PollingTripUpdaterParameters(
    String configRef,
    int frequencySec,
    boolean fuzzyTripMatching,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    String feedId,
    String httpSourceUrl,
    String fileSource
  ) {
    this.configRef = configRef;
    this.frequencySec = frequencySec;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.backwardsDelayPropagationType = backwardsDelayPropagationType;
    this.feedId = feedId;
    this.httpSourceUrl = httpSourceUrl;
    this.fileSource = fileSource;
  }

  @Override
  public int frequencySec() {
    return frequencySec;
  }

  @Override
  public String getUrl() {
    return httpSourceUrl;
  }

  @Override
  public String configRef() {
    return configRef;
  }

  public String getFeedId() {
    return feedId;
  }

  boolean fuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  BackwardsDelayPropagationType getBackwardsDelayPropagationType() {
    return backwardsDelayPropagationType;
  }

  GtfsRealtimeFileTripUpdateSource.Parameters fileSourceParameters() {
    return new GtfsRealtimeFileTripUpdateSource.Parameters() {
      @Override
      public String getFeedId() {
        return feedId;
      }

      @Override
      public String getFile() {
        return fileSource;
      }
    };
  }

  GtfsRealtimeHttpTripUpdateSource.Parameters httpSourceParameters() {
    return new GtfsRealtimeHttpTripUpdateSource.Parameters() {
      @Override
      public String getFeedId() {
        return feedId;
      }

      @Override
      public String getUrl() {
        return httpSourceUrl;
      }
    };
  }
}
