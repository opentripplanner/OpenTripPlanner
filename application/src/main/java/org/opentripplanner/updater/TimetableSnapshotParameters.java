package org.opentripplanner.updater;

import java.time.Duration;

/**
 * {@link
 * org.opentripplanner.standalone.config.routerconfig.UpdatersConfig#timetableUpdates(org.opentripplanner.standalone.config.framework.json.NodeAdapter)}
 */
public record TimetableSnapshotParameters(Duration maxSnapshotFrequency, boolean purgeExpiredData) {
  public static final TimetableSnapshotParameters DEFAULT = new TimetableSnapshotParameters(
    Duration.ofSeconds(1),
    true
  );

  public static final TimetableSnapshotParameters PUBLISH_IMMEDIATELY = new TimetableSnapshotParameters(
    Duration.ZERO,
    false
  );

  /* Factory functions, used instead of a builder - useful in tests. */

  public TimetableSnapshotParameters withMaxSnapshotFrequency(Duration maxSnapshotFrequency) {
    return new TimetableSnapshotParameters(maxSnapshotFrequency, this.purgeExpiredData);
  }

  public TimetableSnapshotParameters withPurgeExpiredData(boolean purgeExpiredData) {
    return new TimetableSnapshotParameters(this.maxSnapshotFrequency, purgeExpiredData);
  }
}
