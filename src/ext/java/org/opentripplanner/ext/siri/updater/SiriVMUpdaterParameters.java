package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public class SiriVMUpdaterParameters implements PollingGraphUpdaterParameters {
  private final String configRef;
  private final String feedId;
  private final int logFrequency;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean fuzzyTripMatching;
  private final boolean blockReadinessUntilInitialized;

  // Source parameters
  private final String url;
  private final String requestorRef;
  private final int frequencySec;
  private final int timeoutSec;

  public SiriVMUpdaterParameters(
      String configRef,
      String feedId,
      int logFrequency,
      int maxSnapshotFrequencyMs,
      boolean purgeExpiredData,
      boolean fuzzyTripMatching,
      boolean blockReadinessUntilInitialized,
      String url,
      String requestorRef,
      int frequencySec,
      int timeoutSec
  ) {
    this.configRef = configRef;
    this.feedId = feedId;
    this.logFrequency = logFrequency;
    this.maxSnapshotFrequencyMs = maxSnapshotFrequencyMs;
    this.purgeExpiredData = purgeExpiredData;
    this.fuzzyTripMatching = fuzzyTripMatching;
    this.blockReadinessUntilInitialized = blockReadinessUntilInitialized;
    this.url = url;
    this.requestorRef = requestorRef;
    this.frequencySec = frequencySec;
    this.timeoutSec = timeoutSec;
  }

  @Override
  public int getFrequencySec() {
    return frequencySec;
  }

  @Override
  public String getConfigRef() { return configRef; }

  public String getFeedId() { return feedId; }
  public int getLogFrequency() { return logFrequency; }
  public int getMaxSnapshotFrequencyMs() { return maxSnapshotFrequencyMs; }
  public boolean purgeExpiredData() { return purgeExpiredData; }
  public boolean fuzzyTripMatching() { return fuzzyTripMatching; }
  public boolean blockReadinessUntilInitialized() { return blockReadinessUntilInitialized; }

  public SiriVMHttpTripUpdateSource.Parameters sourceParameters() {
    return new SiriVMHttpTripUpdateSource.Parameters() {
      @Override public String getRequestorRef() { return requestorRef; }
      @Override public String getFeedId() { return feedId; }
      @Override public int getTimeoutSec() { return timeoutSec; }
      @Override public String getUrl() { return url; }
    };
  }
}
