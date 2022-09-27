package org.opentripplanner.updater.stoptime;

import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class PollingTripUpdaterParameters
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {

  private final String configRef;
  private final int frequencySec;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean fuzzyTripMatching;
  private final BackwardsDelayPropagationType backwardsDelayPropagationType;

  // Source
  private final DataSourceType sourceType;
  private final String feedId;
  private final String httpSourceUrl;
  private final String fileSource;

  public PollingTripUpdaterParameters(
    String configRef,
    int frequencySec,
    int maxSnapshotFrequencyMs,
    boolean purgeExpiredData,
    boolean fuzzyTripMatching,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    DataSourceType sourceType,
    String feedId,
    String httpSourceUrl,
    String fileSource
  ) {
    this.configRef = configRef;
    this.frequencySec = frequencySec;
    this.maxSnapshotFrequencyMs = maxSnapshotFrequencyMs;
    this.purgeExpiredData = purgeExpiredData;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.backwardsDelayPropagationType = backwardsDelayPropagationType;
    this.sourceType = sourceType;
    this.feedId = feedId;
    this.httpSourceUrl = httpSourceUrl;
    this.fileSource = fileSource;
  }

  @Override
  public int getFrequencySec() {
    return frequencySec;
  }

  @Override
  public String getUrl() {
    return httpSourceUrl;
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }

  public DataSourceType getSourceType() {
    return sourceType;
  }

  public String getFeedId() {
    return feedId;
  }

  boolean purgeExpiredData() {
    return purgeExpiredData;
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
