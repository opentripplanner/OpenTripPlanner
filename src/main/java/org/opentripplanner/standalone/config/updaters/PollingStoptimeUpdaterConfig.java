package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdater;

public class PollingStoptimeUpdaterConfig extends PollingGraphUpdaterConfig
    implements PollingStoptimeUpdater.Parameters {

  private final String feedId;
  private final int logFrequency;
  private final int maxSnapshotFrequencyMs;
  private final boolean purgeExpiredData;
  private final boolean fuzzyTripMatching;

  public PollingStoptimeUpdaterConfig(String configRef, NodeAdapter c) {
    super(configRef, c);
    feedId = c.asText("feedId", null);
    logFrequency = c.asInt("logFrequency", -1);
    maxSnapshotFrequencyMs = c.asInt("maxSnapshotFrequencyMs", -1);
    purgeExpiredData = c.asBoolean("purgeExpiredData", false);
    fuzzyTripMatching = c.asBoolean("fuzzyTripMatching", false);
  }

  public String getFeedId() { return feedId; }

  public int getLogFrequency() { return logFrequency; }

  public int getMaxSnapshotFrequencyMs() { return maxSnapshotFrequencyMs; }

  public boolean purgeExpiredData() { return purgeExpiredData; }

  public boolean fuzzyTripMatching() { return fuzzyTripMatching; }
}
