package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesStringBuilder;
import org.opentripplanner.transit.service.TransitService;

/**
 * A convenience class for fetching data for a trip on a specific service date. It acts as a thin
 * layer on top of the TransitService.
 */
public class TripOnDateDataFetcher {

  private final FeedScopedId tripId;
  private final TransitService transitService;
  private final LocalDate serviceDate;

  // The trip is lazily initialized when needed
  private Trip trip;

  public TripOnDateDataFetcher(
    TransitService transitService,
    FeedScopedId tripId,
    LocalDate serviceDate
  ) {
    this.tripId = tripId;
    this.transitService = transitService;
    this.serviceDate = serviceDate;
  }

  public Trip trip() {
    if (trip != null) {
      return trip;
    }
    trip = transitService.getTrip(tripId);
    return trip;
  }

  /**
   * Get the TripPattern for the given trip on date. Returns the realtime updated pattern if it
   * exists and otherwise the scheduled.
   */
  public TripPattern tripPattern() {
    return transitService.findPattern(trip(), serviceDate);
  }

  /**
   * Get the scheduled tripPattern for the given trip
   */
  public TripPattern scheduledTripPattern() {
    return transitService.findPattern(trip());
  }

  /**
   *  Get realtime TripTimes for the trip on the date
   */
  public TripTimes tripTimes() {
    var timetable = transitService.findTimetable(tripPattern(), serviceDate);
    return timetable.getTripTimes(trip());
  }

  /**
   *  Get scheduled TripTimes for the trip
   */
  public TripTimes scheduledTripTimes() {
    var timetable = scheduledTripPattern().getScheduledTimetable();
    return timetable.getTripTimes(trip());
  }

  public RealTimeState realTimeState() {
    return tripTimes().getRealTimeState();
  }

  /**
   * Get a string representation of the realtime tripTimes
   */
  public String showTimetable() {
    return TripTimesStringBuilder.encodeTripTimes(tripTimes(), tripPattern());
  }

  /**
   * Get a string representation of the scheduled tripTimes
   */
  public String showScheduledTimetable() {
    return TripTimesStringBuilder.encodeTripTimes(scheduledTripTimes(), scheduledTripPattern());
  }
}
