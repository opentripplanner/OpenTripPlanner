package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.ext.siri.updater.SiriVMUpdater;
import org.opentripplanner.standalone.config.NodeAdapter;

public class SiriVMUpdaterConfig extends PollingGraphUpdaterConfig
    implements SiriVMUpdater.Parameters {

  private final String feedId;
  private final int logFrequency;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean fuzzyTripMatching;
  private final boolean blockReadinessUntilInitialized;

  public SiriVMUpdaterConfig(String configRef, NodeAdapter c) {
    super(configRef, c);
    feedId = c.asText("feedId", null);
    logFrequency = c.asInt("logFrequency", -1);
    maxSnapshotFrequencyMs = c.asInt("maxSnapshotFrequencyMs", -1);
    purgeExpiredData = c.asBoolean("purgeExpiredData", false);
    fuzzyTripMatching = c.asBoolean("fuzzyTripMatching", false);
    blockReadinessUntilInitialized = c.asBoolean("blockReadinessUntilInitialized", false);
  }


  public String getFeedId() { return feedId; }

  public int getLogFrequency() { return logFrequency; }

  public int getMaxSnapshotFrequencyMs() { return maxSnapshotFrequencyMs; }

  public boolean purgeExpiredData() { return purgeExpiredData; }

  public boolean fuzzyTripMatching() { return fuzzyTripMatching; }

  public boolean blockReadinessUntilInitialized() { return blockReadinessUntilInitialized; }
}
