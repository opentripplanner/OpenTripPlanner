package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.ext.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.standalone.config.NodeAdapter;

public class SiriETUpdaterConfig extends PollingGraphUpdaterConfig
    implements SiriETUpdater.Parameters {

  private final String feedId;
  private final int logFrequency;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean blockReadinessUntilInitialized;

  public SiriETUpdaterConfig(String configRef, NodeAdapter c) {
    super(configRef, c);
    feedId = c.asText("feedId", null);
    logFrequency = c.asInt("logFrequency", -1);
    maxSnapshotFrequencyMs = c.asInt("maxSnapshotFrequencyMs", -1);
    purgeExpiredData = c.asBoolean("purgeExpiredData", false);
    blockReadinessUntilInitialized = c.asBoolean("blockReadinessUntilInitialized", false);
  }

  @Override
  public String getFeedId() { return feedId; }

  @Override
  public int getLogFrequency() { return logFrequency; }

  @Override
  public int getMaxSnapshotFrequencyMs() { return maxSnapshotFrequencyMs; }

  @Override
  public boolean purgeExpiredData() { return purgeExpiredData; }

  @Override
  public boolean blockReadinessUntilInitialized() { return blockReadinessUntilInitialized; }
}
