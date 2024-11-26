package org.opentripplanner.raptor.spi;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * This interface enable the transit layer to override the normal trip search in Raptor. Each {@link
 * RaptorRoute} may provide an instance of this interface so Raptor can ask for a bord-/alight-
 * event with the route as the target.
 * <p>
 * When searching forward the <em>target</em> is the "to" end of the transfer, and the
 * <em>source</em> is the "from" transfer point. For a reverse search the <em>target</em> is "from"
 * and the <em>source</em> is the "to" transfer point.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorConstrainedBoardingSearch<T extends RaptorTripSchedule> {
  /**
   * Check if the current pattern has any constrained transfers for the given target stop position.
   * If not, then Raptor will fall back to a regular trip search.
   * <p>
   * The target stop position is the boarding "to" stop position for a forward search, and the
   * source "from" stop position for a reverse search.
   */
  boolean transferExistTargetStop(int targetStopPos);

  /**
   * Check if the current pattern has any constrained transfers for the given source stop position.
   * If not, then Raptor will fall back to a regular trip search.
   * <p>
   * The source stop position is the alighting "from" stop position for a forward search, and the
   * target "to" stop position for a reverse search.
   */
  boolean transferExistSourceStop(int targetStopPos);

  /**
   * Get the board-/alight-event for the current pattern at the target stop position coming from the
   * source stop and trip with the given source arrival board-/alight time (exclude slack).
   * <p>
   *
   * @return An "empty" event if no target trip is found
   */
  RaptorBoardOrAlightEvent<T> find(
    RaptorTimeTable<T> targetTimetable,
    int transferSlack,
    T sourceTrip,
    int sourceStopIndex,
    int prevTransitArrivalTime,
    int earliestBoardTime
  );
}
