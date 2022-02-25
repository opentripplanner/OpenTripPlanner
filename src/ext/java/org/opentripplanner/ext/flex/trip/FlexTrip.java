package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.*;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This class represents the different variations of what is considered flexible transit, and its
 * subclasses encapsulates the different business logic, which the different types of services
 * adhere to.
 */
public abstract class FlexTrip extends TransitEntity {

  protected final Trip trip;

  public FlexTrip(Trip trip) {
    super(trip.getId());
    this.trip = trip;
  }

  public abstract Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate date, FlexPathCalculator calculator, FlexParameters params
  );

  public abstract Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate date, FlexPathCalculator calculator, FlexParameters params
  );

  public abstract int earliestDepartureTime(int departureTime, int fromStopIndex, int toStopIndex, int flexTime);

  public abstract int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex, int flexTime);

  /**
   * Returns all the stops that are in this trip.
   *
   * Note that they are in no specific order and don't correspond 1-to-1 to the stop times of the
   * trip.
   *
   * Location groups are expanded into their constituent stops.
   */
  public abstract Set<StopLocation> getStops();

  public Trip getTrip() {
    return trip;
  }

  public abstract BookingInfo getDropOffBookingInfo(int i);

  public abstract BookingInfo getPickupBookingInfo(int i);

  public abstract boolean isBoardingPossible(NearbyStop stop);

  public abstract boolean isAlightingPossible(NearbyStop stop);

  public static boolean containsFlexStops(List<StopTime> stopTimes) {
    return stopTimes.stream().map(StopTime::getStop).anyMatch(FlexTrip::isFlexStop);
  }

  public static boolean isFlexStop(StopLocation stop) {
    return stop instanceof FlexLocationGroup || stop instanceof FlexStopLocation;
  }
}
