package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
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

  private static final long serialVersionUID = 8819000771336287893L;

  protected final Trip trip;
  
  public FlexTrip(Trip trip) {
    super(trip.getId());
    this.trip = trip;
  }

  public abstract Stream<FlexAccessTemplate> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate date, FlexPathCalculator calculator
  );

  public abstract Stream<FlexEgressTemplate> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate date, FlexPathCalculator calculator
  );

  // the 95% CI for travel time on this trip. Use this for connections and other things that 
  // need more certainty about the arrival/departure time
  public abstract int getSafeTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex);

  // the "usual" for travel time on this trip. Use this for display and other things that 
  // are supposed to be more the norm vs. the "worst case" scenario
  public abstract int getMeanTotalTime(FlexPath streetPath, int fromStopIndex, int toStopIndex);

  // this method returns seconds since midnight
  public abstract int earliestDepartureTime(int departureTime, int fromStopIndex, int toStopIndex);

  // this method returns seconds since midnight
  public abstract int latestArrivalTime(int arrivalTime, int fromStopIndex, int toStopIndex);

  public abstract Collection<StopLocation> getStops();

  public Trip getTrip() {
    return trip;
  }

  public abstract BookingInfo getBookingInfo(int i);
}
