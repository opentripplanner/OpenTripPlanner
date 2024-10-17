package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Class that contains a {@link StopLocation} on a {@link Trip}.
 */
public class StopOnTripModel {

  /**
   * Stop that should be on the trip but technically it's possible that it isn't or that it's null.
   */
  private final StopLocation stop;

  /**
   * Trip that should contain the stop but technically it's possible that the stop isn't on the trip
   * and it's also possible that the trip is null.
   */
  private final Trip trip;

  public StopOnTripModel(StopLocation stop, Trip trip) {
    this.stop = stop;
    this.trip = trip;
  }

  public StopLocation getStop() {
    return stop;
  }

  public Trip getTrip() {
    return trip;
  }
}
