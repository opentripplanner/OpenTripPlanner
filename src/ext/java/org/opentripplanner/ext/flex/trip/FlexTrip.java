package org.opentripplanner.ext.flex.trip;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

/**
 * This class represents the different variations of what is considered flexible transit, and its
 * subclasses encapsulates the different business logic, which the different types of services
 * adhere to.
 */
public abstract class FlexTrip<T extends FlexTrip<T, B>, B extends FlexTripBuilder<T, B>>
  extends AbstractTransitEntity<T, B> {

  public static int STOP_INDEX_NOT_FOUND = -1;

  private final Trip trip;

  FlexTrip(FlexTripBuilder<T, B> builder) {
    super(builder.getId());
    this.trip = builder.trip();
  }

  public static boolean containsFlexStops(List<StopTime> stopTimes) {
    return stopTimes.stream().map(StopTime::getStop).anyMatch(FlexTrip::isFlexStop);
  }

  public static boolean isFlexStop(StopLocation stop) {
    return stop instanceof GroupStop || stop instanceof AreaStop;
  }

  /**
   * Earliest departure time from boardStopPosition to alightStopPosition, which departs after departureTime,
   * and for which the flex trip has a duration of flexTime seconds.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int earliestDepartureTime(
    int departureTime,
    int boardStopPosition,
    int alightStopPosition,
    int flexTripDurationSeconds
  );

  /**
   * Earliest departure time from boardStopPosition.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int earliestDepartureTime(int stopIndex);

  /**
   * Latest arrival time to alightStopPosition from boardStopPosition, which arrives before arrivalTime,
   * and for which the flex trip has a duration of flexTime seconds.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int latestArrivalTime(
    int arrivalTime,
    int boardStopPosition,
    int alightStopPosition,
    int tripDurationSeconds
  );

  /**
   * Latest arrival time to alightStopPosition.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int latestArrivalTime(int stopIndex);

  /**
   * Return number-of-stops this trip visit.
   */
  public abstract int numberOfStops();

  /**
   * Returns all the stops that are in this trip.
   * <p>
   * Note that they are in no specific order and don't correspond 1-to-1 to the stop times of the
   * trip.
   * <p>
   * Location groups are expanded into their constituent stops.
   */
  public abstract Set<StopLocation> getStops();

  /**
   * Return a stop at given stop-index. Note! The visited order may not be the same as the
   * indexing order.
   */
  public abstract StopLocation getStop(int stopIndex);

  public Trip getTrip() {
    return trip;
  }

  public abstract BookingInfo getDropOffBookingInfo(int i);

  public abstract BookingInfo getPickupBookingInfo(int i);

  public abstract PickDrop getBoardRule(int i);

  public abstract PickDrop getAlightRule(int i);

  public abstract boolean isBoardingPossible(StopLocation stop);

  public abstract boolean isAlightingPossible(StopLocation stop);

  /**
   * Find the first stop-position matching the given {@code fromStop} where
   * boarding is allowed.
   *
   * @return stop position in the pattern or {@link #STOP_INDEX_NOT_FOUND} if not found.
   */
  public abstract int findBoardIndex(StopLocation fromStop);

  /**
   * Find the first stop-position matching the given {@code toStop} where
   * alighting is allowed.
   *
   * @return the stop position in the pattern or {@link #STOP_INDEX_NOT_FOUND} if not found.
   */
  public abstract int findAlightIndex(StopLocation toStop);

  /**
   * Allow each FlexTrip type to decorate or replace the router defaultCalculator.
   */
  public abstract FlexPathCalculator decorateFlexPathCalculator(
    FlexPathCalculator defaultCalculator
  );

  @Override
  public boolean sameAs(@Nonnull T other) {
    return getId().equals(other.getId()) && Objects.equals(trip, other.getTrip());
  }
}
