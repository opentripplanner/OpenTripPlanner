package org.opentripplanner.updater.trip.gtfs;

import java.util.BitSet;
import java.util.List;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Finds the calls of a GTFS-RT {@code REPLACEMENT} trip that do not exist in the scheduled pattern
 * of the trip, so they can be marked as extra calls.
 * <p>
 * GTFS-RT has no way to flag a stop as extra, a replacement is just a complete new list of stops.
 * To be consistent with {@link org.opentripplanner.model.TripTimeOnDate#getScheduledStop}, which
 * maps real-time positions to scheduled positions by subtracting the number of preceding extra
 * calls, the flags are only derived when the replacement is a pure <em>insertion</em> into the
 * scheduled pattern: every scheduled stop must still be present, in the same order. If stops were
 * removed or reordered no call is flagged, because we cannot tell which call replaces which.
 * <p>
 * Scheduled stops are matched greedily from the start, so if a stop is visited more than once, the
 * earliest occurrence is considered the scheduled call.
 */
final class ReplacementExtraCalls {

  private ReplacementExtraCalls() {}

  /**
   * @return the positions in {@code realTimeStops} that are not part of the scheduled pattern, or
   * an empty set if the scheduled stops cannot be matched as an ordered subsequence.
   */
  static BitSet find(
    List<? extends StopLocation> scheduledStops,
    List<? extends StopLocation> realTimeStops
  ) {
    var extraCalls = new BitSet(realTimeStops.size());
    int scheduledPos = 0;
    for (int i = 0; i < realTimeStops.size(); i++) {
      if (
        scheduledPos < scheduledStops.size() &&
        scheduledStops.get(scheduledPos).getId().equals(realTimeStops.get(i).getId())
      ) {
        scheduledPos++;
      } else {
        extraCalls.set(i);
      }
    }
    return scheduledPos == scheduledStops.size() ? extraCalls : new BitSet();
  }
}
