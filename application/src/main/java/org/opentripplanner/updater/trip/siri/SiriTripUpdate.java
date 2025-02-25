package org.opentripplanner.updater.trip.siri;

import java.time.LocalDate;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * This is a DTO used internally in the SIRI code for holding on to realtime trip update information
 */
public sealed interface SiriTripUpdate
  permits SiriTripUpdate.SiriAddTrip, SiriTripUpdate.SiriModifyTrip {
  TripTimes tripTimes();
  LocalDate serviceDate();

  /**
   * A message containing information for modifying an existing trip.
   *
   * @param stopPattern            The stop pattern for the modified trip.
   * @param tripTimes              The updated trip times for the modified trip.
   * @param serviceDate            The service date for which this update applies (updates are valid
   *                               only for one service date)
   * @param dataSource             The dataSource of the real-time update.
   */
  record SiriModifyTrip(
    StopPattern stopPattern,
    RealTimeTripTimes tripTimes,
    LocalDate serviceDate,
    @Nullable String dataSource
  )
    implements SiriTripUpdate {}

  /**
   * A message with information for adding a new trip
   *
   * @param stopPattern            The stop pattern to which belongs the created trip.
   * @param tripTimes              The trip times for the created trip.
   * @param serviceDate            The service date for which this update applies (updates are valid
   *                               only for one service date)
   * @param addedTripOnServiceDate TripOnServiceDate corresponding to the new trip.
   * @param addedTripPattern       The new trip pattern for the new trip.
   * @param routeCreation          true if an added trip cannot be registered under an existing route
   *                               and a new route must be created.
   * @param dataSource             The dataSource of the real-time update.
   */
  record SiriAddTrip(
    StopPattern stopPattern,
    TripTimes tripTimes,
    LocalDate serviceDate,
    TripOnServiceDate addedTripOnServiceDate,
    TripPattern addedTripPattern,
    boolean routeCreation,
    @Nullable String dataSource
  )
    implements SiriTripUpdate {}
}
