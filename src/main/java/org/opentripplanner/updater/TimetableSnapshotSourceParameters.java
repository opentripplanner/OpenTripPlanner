package org.opentripplanner.updater;

import java.time.Duration;

/**
 * {@link
 * org.opentripplanner.standalone.config.routerconfig.UpdatersConfig#timetableUpdates(org.opentripplanner.standalone.config.framework.json.NodeAdapter)}
 */
public record TimetableSnapshotSourceParameters(
  Duration maxSnapshotFrequency,
  boolean purgeExpiredData
) {
  public static final TimetableSnapshotSourceParameters DEFAULT = new TimetableSnapshotSourceParameters(
    Duration.ofSeconds(1),
    true
  );

  /* Factory functions, used instead of a builder - useful in tests. */

  public TimetableSnapshotSourceParameters withMaxSnapshotFrequency(Duration maxSnapshotFrequency) {
    return new TimetableSnapshotSourceParameters(maxSnapshotFrequency, this.purgeExpiredData);
  }

  public TimetableSnapshotSourceParameters withPurgeExpiredData(boolean purgeExpiredData) {
    return new TimetableSnapshotSourceParameters(this.maxSnapshotFrequency, purgeExpiredData);
  }
}
