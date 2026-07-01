package org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.InvalidRoutingInputException;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

/**
 * Resolves a trip location from timetable based on the trip and service date, the id of the stop
 * (or station), and an optional aimed departure time.
 */
public class TripLocationResolver {

  private final TransitService transitService;

  public TripLocationResolver(TransitService transitService) {
    this.transitService = transitService;
  }

  /**
   * Resolve a {@link LocationInTripPatternReference} from timetable data.
   * @param tripAndServiceDate The trip and service date for this location.
   * @param stopOrStationId The stop or station id. The stop can be a regular stop or station,
   *                        but not a multimodal stop or a group of stops.
   * @param aimedDepartureTime An optional departure time used to disambiguate for trip's that pass
   *                           by the same stop multiple times - forming a ring line. When provided,
   *                           it must match the actual departure time of the stop exactly.
   */
  public LocationInTripPatternReference resolve(
    TripAndServiceDate tripAndServiceDate,
    FeedScopedId stopOrStationId,
    @Nullable Integer aimedDepartureTime
  ) {
    var trip = tripAndServiceDate.trip();
    var serviceDate = tripAndServiceDate.serviceDate();

    var tripPattern = transitService.findPattern(trip, serviceDate);
    if (tripPattern == null) {
      throw new InvalidRoutingInputException(
        "No pattern found for trip %s on date %s".formatted(trip.getId(), serviceDate)
      );
    }

    return findLocationInTripPattern(
      tripPattern,
      stopOrStationId,
      trip,
      serviceDate,
      aimedDepartureTime
    );
  }

  private LocationInTripPatternReference findLocationInTripPattern(
    TripPattern tripPattern,
    FeedScopedId stopOrStationId,
    Trip trip,
    LocalDate serviceDate,
    @Nullable Integer aimedDepartureTime
  ) {
    var tripTimes = transitService.findTimetable(tripPattern, serviceDate).getTripTimes(trip);
    if (tripTimes == null) {
      throw new InvalidRoutingInputException(
        "Trip %s not found in timetable for pattern %s on date %s".formatted(
          trip.getId(),
          tripPattern.getId(),
          serviceDate
        )
      );
    }

    var locations = matchingLocations(tripPattern, stopOrStationId, tripTimes, aimedDepartureTime);

    if (locations.isEmpty()) {
      if (aimedDepartureTime != null) {
        throw new InvalidRoutingInputException(
          "No stop %s with the provided departure time found in pattern for trip %s".formatted(
            stopOrStationId,
            trip.getId()
          )
        );
      }
      throw new InvalidRoutingInputException(
        "Stop or station %s not found in pattern for trip %s".formatted(
          stopOrStationId,
          trip.getId()
        )
      );
    }
    if (locations.size() > 1) {
      throw new RoutingValidationException(
        List.of(
          new RoutingError(
            RoutingErrorCode.TRIP_LOCATION_MISSING_SCHEDULED_DEPARTURE_TIME,
            InputField.FROM_PLACE
          )
        )
      );
    }

    var locationInTripPattern = locations.getFirst();
    int lastStopPos = tripPattern.numberOfStops() - 1;
    if (locationInTripPattern.stopPositionInPattern() == lastStopPos) {
      throw new InvalidRoutingInputException(
        "Cannot board at the last stop %s of trip %s — no further travel is possible".formatted(
          stopOrStationId,
          trip.getId()
        )
      );
    }

    return locationInTripPattern;
  }

  /**
   * If targetSeconds is non-null, only stops with a matching scheduled departure time are
   * returned, otherwise all stops matching the stop or station id are returned.
   */
  private List<LocationInTripPatternReference> matchingLocations(
    TripPattern tripPattern,
    FeedScopedId stopOrStationId,
    TripTimes tripTimes,
    @Nullable Integer targetSeconds
  ) {
    var locations = new ArrayList<LocationInTripPatternReference>();
    for (int stopPos = 0; stopPos < tripPattern.numberOfStops(); stopPos++) {
      var stop = tripPattern.getStop(stopPos);
      if (
        stop.getId().equals(stopOrStationId) || stop.getStationOrStopId().equals(stopOrStationId)
      ) {
        int departureTime = tripTimes.getScheduledDepartureTime(stopPos);
        if (targetSeconds != null && departureTime != targetSeconds) {
          continue;
        }
        locations.add(new LocationInTripPatternReference(stop.getIndex(), stopPos, departureTime));
      }
    }
    return locations;
  }
}
