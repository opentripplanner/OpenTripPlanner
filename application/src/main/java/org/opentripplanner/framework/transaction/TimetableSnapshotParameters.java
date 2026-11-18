package org.opentripplanner.framework.transaction;

import java.time.Duration;

/**
 * Parameters governing the behaviour of the timetable snapshot.
 * <p>
 * This class lives in the {@code transaction} package even though only {@code maxSnapshotFrequency}
 * is directly related to transaction concerns, while {@code purgeExpiredData} belongs to the
 * updater layer. The two parameters cannot be split into separate configuration classes because
 * doing so would break backwards compatibility of the configuration format. Placing both parameters
 * here is the only way to avoid circular dependencies between the {@code transaction} and
 * {@code updater} packages.
 */
public record TimetableSnapshotParameters(Duration maxSnapshotFrequency, boolean purgeExpiredData) {
  public static final TimetableSnapshotParameters DEFAULT = new TimetableSnapshotParameters(
    Duration.ofSeconds(1),
    true
  );

  public static final TimetableSnapshotParameters PUBLISH_IMMEDIATELY =
    new TimetableSnapshotParameters(Duration.ZERO, false);

  /* Factory functions, used instead of a builder - useful in tests. */

  public TimetableSnapshotParameters withMaxSnapshotFrequency(Duration maxSnapshotFrequency) {
    return new TimetableSnapshotParameters(maxSnapshotFrequency, this.purgeExpiredData);
  }

  public TimetableSnapshotParameters withPurgeExpiredData(boolean purgeExpiredData) {
    return new TimetableSnapshotParameters(this.maxSnapshotFrequency, purgeExpiredData);
  }
}
