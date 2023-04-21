package org.opentripplanner.ext.flex.trip;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This class represents the different variations of what is considered flexible transit, and its
 * subclasses encapsulates the different business logic, which the different types of services
 * adhere to.
 */
public abstract class FlexTrip<T extends FlexTrip<T, B>, B extends FlexTripBuilder<T, B>>
  extends AbstractTransitEntity<T, B> {

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

  public abstract Stream<FlexAccessTemplate> getFlexAccessTemplates(
    NearbyStop access,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  );

  public abstract Stream<FlexEgressTemplate> getFlexEgressTemplates(
    NearbyStop egress,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  );

  /**
   * Earliest departure time from fromStopIndex to toStopIndex, which departs after departureTime,
   * and for which the flex trip has a duration of flexTime seconds.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int earliestDepartureTime(
    int departureTime,
    int fromStopIndex,
    int toStopIndex,
    int flexTripDurationSeconds
  );

  /**
   * Earliest departure time from fromStopIndex.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int earliestDepartureTime(int stopIndex);

  /**
   * Latest arrival time to toStopIndex from fromStopIndex, which arrives before arrivalTime,
   * and for which the flex trip has a duration of flexTime seconds.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int latestArrivalTime(
    int arrivalTime,
    int fromStopIndex,
    int toStopIndex,
    int tripDurationSeconds
  );

  /**
   * Latest arrival time to toStopIndex.
   *
   * @return {@link StopTime#MISSING_VALUE} is returned if a departure does not exist.
   */
  public abstract int latestArrivalTime(int stopIndex);

  /**
   * Returns all the stops that are in this trip.
   * <p>
   * Note that they are in no specific order and don't correspond 1-to-1 to the stop times of the
   * trip.
   * <p>
   * Location groups are expanded into their constituent stops.
   */
  public abstract Set<StopLocation> getStops();

  public Trip getTrip() {
    return trip;
  }

  public abstract BookingInfo getDropOffBookingInfo(int i);

  public abstract BookingInfo getPickupBookingInfo(int i);

  public abstract PickDrop getBoardRule(int i);

  public abstract PickDrop getAlightRule(int i);

  public abstract boolean isBoardingPossible(NearbyStop stop);

  public abstract boolean isAlightingPossible(NearbyStop stop);

  @Override
  public boolean sameAs(@Nonnull T other) {
    return getId().equals(other.getId()) && Objects.equals(trip, other.getTrip());
  }
}
