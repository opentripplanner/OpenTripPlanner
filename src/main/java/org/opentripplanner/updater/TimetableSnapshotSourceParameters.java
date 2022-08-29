package org.opentripplanner.updater;

/**
 * @param logFrequency           Log a status message for every successfully applied trip updates.
 *                               Apply to GTFS-RT updates only.
 * @param maxSnapshotFrequencyMs If a timetable snapshot is requested less than this number of
 *                               milliseconds after the previous snapshot, then return the same
 *                               instance. Throttles the potentially resource-consuming task of
 *                               duplicating a TripPattern → Timetable map and indexing the new
 *                               Timetables. Apply to GTFS-RT and Siri updates.
 * @param purgeExpiredData       Should expired realtime data be purged from the graph. Apply to
 *                               GTFS-RT and Siri updates.
 */
public record TimetableSnapshotSourceParameters(
  int logFrequency,
  int maxSnapshotFrequencyMs,
  boolean purgeExpiredData
) {
  public static final TimetableSnapshotSourceParameters DEFAULT = new TimetableSnapshotSourceParameters(
    2000,
    1000,
    true
  );

  /* Factory functions, used instead of a builder - useful in tests. */

  public TimetableSnapshotSourceParameters withMaxSnapshotFrequencyMs(int maxSnapshotFrequencyMs) {
    return new TimetableSnapshotSourceParameters(
      this.logFrequency,
      maxSnapshotFrequencyMs,
      this.purgeExpiredData
    );
  }

  public TimetableSnapshotSourceParameters withPurgeExpiredData(boolean purgeExpiredData) {
    return new TimetableSnapshotSourceParameters(
      this.logFrequency,
      this.maxSnapshotFrequencyMs,
      purgeExpiredData
    );
  }
}
