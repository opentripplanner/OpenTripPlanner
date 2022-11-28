package org.opentripplanner.raptor.spi;

import javax.annotation.Nullable;

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
public interface RaptorConstrainedTripScheduleBoardingSearch<T extends RaptorTripSchedule> {
  /**
   * Check if the current pattern have any guaranteed transfers for the given stop position in
   * pattern. If not, then Raptor will fall back to a regular trip search.
   */
  boolean transferExist(int targetStopPos);

  /**
   * Get the board-/alight-event for the current pattern at the target stop position coming from the
   * source stop and trip with the given source arrival board-/alight time (exclude slack).
   * <p>
   *
   * @return {@code null} if no target trip is found
   */
  @Nullable
  RaptorTripScheduleBoardOrAlightEvent<T> find(
    RaptorTimeTable<T> targetTimetable,
    int transferSlack,
    T sourceTrip,
    int sourceStopIndex,
    int prevTransitArrivalTime,
    int earliestBoardTime
  );
}
