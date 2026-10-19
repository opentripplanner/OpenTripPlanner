package org.opentripplanner.raptor.api.model;

/**
 * This class allows you to board a specific trip at a given stop. The trip is identified by the
 * route index and trip schedule index. A typical use-case for this is when you want to start a
 * trip on-board, meaning that one is already on the vehicle when the path starts. The returned
 * paths will start with a zero duration access and a boarding at the given stop.
 */
public interface RaptorStartOnBoardAccess extends RaptorAccessEgress {
  /**
   * Return the trip boarding this access is required to use.
   */
  RaptorTripScheduleStopPosition tripBoarding();

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
  default boolean arrivedOnStreet() {
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
