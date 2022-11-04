package org.opentripplanner.updater;

/**
 * {@link org.opentripplanner.standalone.config.routerconfig.UpdatersConfig#timetableUpdates(org.opentripplanner.standalone.config.framework.json.NodeAdapter)}
 */
public record TimetableSnapshotSourceParameters(
  int maxSnapshotFrequencyMs,
  boolean purgeExpiredData
) {
  public static final TimetableSnapshotSourceParameters DEFAULT = new TimetableSnapshotSourceParameters(
    1000,
    true
  );

  /* Factory functions, used instead of a builder - useful in tests. */

  public TimetableSnapshotSourceParameters withMaxSnapshotFrequencyMs(int maxSnapshotFrequencyMs) {
    return new TimetableSnapshotSourceParameters(maxSnapshotFrequencyMs, this.purgeExpiredData);
  }

  public TimetableSnapshotSourceParameters withPurgeExpiredData(boolean purgeExpiredData) {
    return new TimetableSnapshotSourceParameters(this.maxSnapshotFrequencyMs, purgeExpiredData);
  }
}
