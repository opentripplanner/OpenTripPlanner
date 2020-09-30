package org.opentripplanner.updater.stoptime;

import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class PollingStoptimeUpdaterParameters implements PollingGraphUpdaterParameters {
  private final String configRef;
  private final int frequencySec;
  private final int logFrequency;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean fuzzyTripMatching;

  // Source
  private final DataSourceType sourceType;
  private final String feedId;
  private final String httpSourceUrl;
  private final String fileSource;

  public PollingStoptimeUpdaterParameters(
      String configRef,
      int frequencySec,
      int logFrequency,
      int maxSnapshotFrequencyMs,
      boolean purgeExpiredData,
      boolean fuzzyTripMatching,
      DataSourceType sourceType,
      String feedId,
      String httpSourceUrl,
      String fileSource
  ) {
    this.configRef = configRef;
    this.frequencySec = frequencySec;
    this.logFrequency = logFrequency;
    this.maxSnapshotFrequencyMs = maxSnapshotFrequencyMs;
    this.purgeExpiredData = purgeExpiredData;
    this.fuzzyTripMatching = fuzzyTripMatching;
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
  public String getConfigRef() {
    return configRef;
  }

  String getFeedId() {
    return feedId;
  }

  int getLogFrequency() {
    return logFrequency;
  }

  int getMaxSnapshotFrequencyMs() {
    return maxSnapshotFrequencyMs;
  }

  boolean purgeExpiredData() {
    return purgeExpiredData;
  }

  boolean fuzzyTripMatching() {
    return fuzzyTripMatching;
  }

  public DataSourceType getSourceType() {
    return sourceType;
  }

  GtfsRealtimeFileTripUpdateSource.Parameters fileSourceParameters() {
    return new GtfsRealtimeFileTripUpdateSource.Parameters() {
      @Override public String getFeedId() { return feedId; }
      @Override public String getFile() { return fileSource; }
    };
  }

  GtfsRealtimeHttpTripUpdateSource.Parameters httpSourceParameters() {
    return new GtfsRealtimeHttpTripUpdateSource.Parameters() {
      @Override public String getFeedId() { return feedId; }
      @Override public String getUrl() { return httpSourceUrl; }
    };
  }
}
