package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class SiriETUpdaterParameters implements PollingGraphUpdaterParameters {
  private final String configRef;
  private final String feedId;
  private final int logFrequency;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean blockReadinessUntilInitialized;

  private final String url;
  private final int frequencySec;

  // Source parameters
  private final String requestorRef;
  private final int timeoutSec;
  private final int previewIntervalMinutes;

  public SiriETUpdaterParameters(
      String configRef,
      String feedId,
      int logFrequency,
      int maxSnapshotFrequencyMs,
      boolean purgeExpiredData,
      boolean blockReadinessUntilInitialized,
      String url,
      int frequencySec,
      String requestorRef,
      int timeoutSec,
      int previewIntervalMinutes
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.logFrequency = logFrequency;
    this.maxSnapshotFrequencyMs = maxSnapshotFrequencyMs;
    this.purgeExpiredData = purgeExpiredData;
    this.blockReadinessUntilInitialized = blockReadinessUntilInitialized;
    this.url = url;
    this.frequencySec = frequencySec;
    this.requestorRef = requestorRef;
    this.timeoutSec = timeoutSec;
    this.previewIntervalMinutes = previewIntervalMinutes;
  }

  public String getFeedId() { return feedId; }

  public int getLogFrequency() { return logFrequency; }

  public int getMaxSnapshotFrequencyMs() { return maxSnapshotFrequencyMs; }

  public boolean purgeExpiredData() { return purgeExpiredData; }

  public boolean blockReadinessUntilInitialized() { return blockReadinessUntilInitialized; }

  @Override public int getFrequencySec() { return frequencySec; }

  @Override public String getConfigRef() { return configRef; }

  public SiriETHttpTripUpdateSource.Parameters sourceParameters() {
    return new SiriETHttpTripUpdateSource.Parameters() {
      @Override public String getUrl() { return url; }
      @Override public String getRequestorRef() { return requestorRef; }
      @Override public String getFeedId() { return feedId; }
      @Override public int getTimeoutSec() { return timeoutSec; }
      @Override public int getPreviewIntervalMinutes() { return previewIntervalMinutes; }
    };
  }
}
