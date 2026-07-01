package org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess;

import java.time.LocalDate;
import java.util.Optional;
import org.opentripplanner.raptor.spi.IntIterators;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTripScheduleReference;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripPatternForDates;
import org.opentripplanner.routing.error.InvalidRoutingInputException;

/**
 * Resolves a {@link RaptorTripScheduleReference} from a {@link TripAndServiceDate} in the Raptor
 * timetable data. Trips in the raptor timetables are indexed by stops, so the resolved stop index
 * from {@link LocationInTripPatternReference} is used to perform the search.
 */
public class TripScheduleIndexResolver {

  private final RaptorRoutingRequestTransitData raptorRequestTransitData;

  /**
   * @param raptorRequestTransitData raptor timetable data
   */
  public TripScheduleIndexResolver(RaptorRoutingRequestTransitData raptorRequestTransitData) {
    this.raptorRequestTransitData = raptorRequestTransitData;
  }

  /**
   * Resolve a {@link RaptorTripScheduleReference} in the Raptor timetable data. A trip schedule
   * reference will be returned if the trip passes through the stop in {@code location} and boarding
   * is possible at the given stop position. If no such trip is found, an
   * {@link InvalidRoutingInputException} is thrown.
   *
   * @param tripAndServiceDate the trip and service date to look up
   * @param location the resolved boarding location, containing the stop index and stop position
   */
  public RaptorTripScheduleReference resolve(
    TripAndServiceDate tripAndServiceDate,
    LocationInTripPatternReference location
  ) {
    var raptorTimetable = getRaptorTimetableForTripAtStop(location.stopIndex(), tripAndServiceDate);
    if (!raptorTimetable.boardingPossibleAt(location.stopPositionInPattern())) {
      throw new InvalidRoutingInputException(
        "Boarding is not allowed at stop position %d for trip %s".formatted(
          location.stopPositionInPattern(),
          tripAndServiceDate.trip()
        )
      );
    }
    var tripSchedule = findTripScheduleInTimetable(raptorTimetable, tripAndServiceDate);
    return raptorRequestTransitData.tripScheduleReference(tripSchedule);
  }

  private TripPatternForDates getRaptorTimetableForTripAtStop(
    int stopIndex,
    TripAndServiceDate tripAndServiceDate
  ) {
    return raptorRequestTransitData
      .activeTripPatternsPerStop(stopIndex)
      .stream()
      .filter(patternForDates ->
        tripPatternForDate(patternForDates, tripAndServiceDate.serviceDate())
          .map(p ->
            p
              .tripTimes()
              .stream()
              .anyMatch(tt -> tt.getTrip().getId().equals(tripAndServiceDate.trip().getId()))
          )
          .orElse(false)
      )
      .findFirst()
      .orElseThrow(() ->
        new InvalidRoutingInputException(
          "No trip pattern on date %s for trip %s".formatted(
            tripAndServiceDate.serviceDate(),
            tripAndServiceDate.trip()
          )
        )
      );
  }

  private Optional<TripPatternForDate> tripPatternForDate(
    TripPatternForDates tripPatternForDates,
    LocalDate serviceDate
  ) {
    var dayIndexIterator = tripPatternForDates.tripPatternForDatesIndexIterator(true);
    while (dayIndexIterator.hasNext()) {
      var dayIndex = dayIndexIterator.next();
      var tripPattern = tripPatternForDates.tripPatternForDate(dayIndex);
      if (tripPattern.getServiceDate().equals(serviceDate)) {
        return Optional.of(tripPattern);
      }
    }
    return Optional.empty();
  }

  private TripSchedule findTripScheduleInTimetable(
    RaptorTimeTable<TripSchedule> timetable,
    TripAndServiceDate tripAndServiceDate
  ) {
    var scheduleIndexIterator = IntIterators.intIncIterator(0, timetable.numberOfTripSchedules());
    while (scheduleIndexIterator.hasNext()) {
      int tripScheduleIndex = scheduleIndexIterator.next();
      var tripSchedule = timetable.getTripSchedule(tripScheduleIndex);
      if (!tripSchedule.getServiceDate().equals(tripAndServiceDate.serviceDate())) {
        continue;
      }
      if (
        tripSchedule
          .getOriginalTripTimes()
          .getTrip()
          .getId()
          .equals(tripAndServiceDate.trip().getId())
      ) {
        return tripSchedule;
      }
    }

    throw new InvalidRoutingInputException(
      "No trip schedule on date %s for trip %s".formatted(
        tripAndServiceDate.serviceDate(),
        tripAndServiceDate.trip()
      )
    );
  }
}
