package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * This class represents the different variations of what is considered flexible transit, and its
 * subclasses encapsulates the different business logic, which the different types of services
 * adhere to.
 */
public abstract class FlexTrip extends TransitEntity {

  protected final Trip trip;

  public FlexTrip(Trip trip) {
    this.trip = trip;
  }

  public abstract Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate date, FlexPathCalculator calculator
  );

  public abstract Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate date, FlexPathCalculator calculator
  );

  public abstract int earliestDepartureTime(int departureTime, int fromStopIndex, int toStopIndex, int flexTime);

  public abstract int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex, int flexTime);

  public abstract Collection<StopLocation> getStops();

  @Override
  public FeedScopedId getId() {
    return trip.getId();
  }

  public Trip getTrip() {
    return trip;
  }
}
