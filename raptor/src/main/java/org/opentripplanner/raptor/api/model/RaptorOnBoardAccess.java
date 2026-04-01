package org.opentripplanner.raptor.api.model;

/**
 * RaptorOnBoardAccess allows you to board a specific trip at a given stop. The trip is
 * identified by the route index and trip schedule index. A typical use-case for this is when you
 * want to start a trip on-board, meaning that one is already on the vehicle when the path starts.
 * The returned paths will start with a zero duration access and a boarding at the given stop.
 */
public interface RaptorOnBoardAccess extends RaptorAccessEgress {
  /**
   * The index of the boarded route
   */
  int routeIndex();

  /**
   * The index of the boarded trip within the route
   */
  int tripScheduleIndex();

  /**
   * The stop position in the route pattern for the board stop. It must refer to the same stop as
   * the {@link #stop()} method. The stop position is required because the stop can be visited twice
   * in case of a circular stop pattern.
   */
  int stopPositionInPattern();

  /**
   * The stop index corresponding to {@link #stopPositionInPattern()}.
   * {@inheritDoc}
   */
  @Override
  int stop();

  /**
   * Since this is an on-board access, the duration until ({@link #stop()}) is 0 seconds.
   * {@inheritDoc}
   */
  @Override
  default int durationInSeconds() {
    return 0;
  }

  @Override
  default int earliestDepartureTime(int requestedDepartureTime) {
    return requestedDepartureTime;
  }

  @Override
  default int latestArrivalTime(int requestedArrivalTime) {
    return requestedArrivalTime;
  }

  @Override
  default boolean hasOpeningHours() {
    return false;
  }

  @Override
  default boolean stopReachedByWalking() {
    return false;
  }

  /**
   * An on-board access does not support riding other transit before the specified boarding
   */
  @Override
  default int numberOfRides() {
    return 0;
  }
}
