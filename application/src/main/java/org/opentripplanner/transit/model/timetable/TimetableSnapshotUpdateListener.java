package org.opentripplanner.transit.model.timetable;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Function;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Listener interface for receiving notifications when a {@link TimetableSnapshot}
 * is updated with new real-time information.
 * <p>
 * Implement this interface and inject the listener into {@link TimetableSnapshot}
 * to receive events after the snapshot is updated.
 */
public interface TimetableSnapshotUpdateListener {
  /**
   * Called after the {@link TimetableSnapshot} has been updated and the commit method has completed.
   * <p>
   * This event allows the implementer to update its own state using the newly updated timetables.
   *
   * @param updatedTimetables a collection of timetables that have been updated
   * @param timetablesForTripPatternId a function to retrieve all timetables for a given trip pattern
   */
  void update(
    Collection<Timetable> updatedTimetables,
    Function<FeedScopedId, SortedSet<Timetable>> timetablesForTripPatternId
  );
}
