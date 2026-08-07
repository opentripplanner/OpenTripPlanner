package org.opentripplanner.updater.trip.siri;

import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_VALID_STOPS;
import static org.opentripplanner.updater.spi.UpdateErrorType.UNKNOWN_STOP;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateErrorType;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matches SIRI TripDescriptors without trip IDs to scheduled GTFS data.
 * <p>
 * Written for Entur (Norway) data, which doesn't yet have complete ID-based matching of SIRI
 * messages to transit model objects. Looks at the last stop and arrival times of the scheduled
 * trip. The matching process will always be applied even where good quality IDs exist — a way to
 * disable it would need to be added separately.
 * <p>
 * Constructed per update task (cheap — only assigns fields). The expensive scheduled-data index
 * lives in {@link SiriFuzzyTripMatcherCache}, which is built once and shared across all SIRI
 * updaters.
 */
public class SiriFuzzyTripMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);

  private final SiriFuzzyTripMatcherCache cache;
  private final TransitService transitService;

  public SiriFuzzyTripMatcher(SiriFuzzyTripMatcherCache cache, TransitService transitService) {
    this.cache = cache;
    this.transitService = transitService;
  }

  /**
   * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
   */
  public TripAndPattern match(
    EstimatedVehicleJourneyWrapper journeyWrapper,
    EntityResolver entityResolver,
    BiFunction<TripPattern, LocalDate, Timetable> getCurrentTimetable,
    BiFunction<FeedScopedId, LocalDate, TripPattern> getNewTripPatternForModifiedTrip
  ) throws UpdateException {
    var calls = journeyWrapper.calls();
    if (calls.isEmpty()) {
      throw UpdateException.of(NO_VALID_STOPS);
    }

    if (calls.getFirst().getAimedDepartureTime() == null) {
      throw UpdateException.of(INVALID_DEPARTURE_TIME);
    }

    Set<Trip> trips = Set.of();
    if (journeyWrapper.vehicleRef().isPresent() && journeyWrapper.isRail()) {
      trips = cache.tripsByInternalPlanningCode(journeyWrapper.vehicleRef().get());
    }

    if (trips.isEmpty()) {
      CallWrapper lastCall = calls.getLast();
      // resolves a scheduled stop point id to a quay (regular stop) if necessary
      // quay ids also work
      RegularStop stop = entityResolver.resolveQuay(lastCall.getStopPointRef());
      if (stop == null) {
        throw UpdateException.of(UNKNOWN_STOP);
      }
      ZonedDateTime arrivalTime = lastCall.getAimedArrivalTime() != null
        ? lastCall.getAimedArrivalTime()
        : lastCall.getAimedDepartureTime();

      if (arrivalTime != null) {
        trips = getMatchingTripsOnStopOrSiblings(stop, arrivalTime);
      }
    }
    if (trips.isEmpty()) {
      throw UpdateException.of(NO_FUZZY_TRIP_MATCH);
    }

    Optional<Route> route = journeyWrapper.lineRef().map(entityResolver::resolveRoute);
    if (route.isPresent()) {
      trips = trips
        .stream()
        .filter(trip -> trip.getRoute().equals(route.get()))
        .collect(Collectors.toSet());
    }

    return getTripAndPatternForJourney(
      trips,
      calls,
      entityResolver,
      getCurrentTimetable,
      getNewTripPatternForModifiedTrip
    );
  }

  /**
   * Returns a match of tripIds that match the provided values.
   */
  public List<FeedScopedId> getTripIdForInternalPlanningCodeServiceDate(
    String internalPlanningCode,
    LocalDate serviceDate
  ) {
    List<FeedScopedId> matches = new ArrayList<>();
    for (Trip trip : cache.tripsByInternalPlanningCode(internalPlanningCode)) {
      Set<LocalDate> serviceDates = transitService
        .getTripCalendars()
        .listServiceDates(trip.getServiceId());
      if (serviceDates.contains(serviceDate)) {
        matches.add(trip.getId());
      }
    }

    return matches;
  }

  private Set<Trip> getMatchingTripsOnStopOrSiblings(
    RegularStop lastStop,
    ZonedDateTime arrivalTime
  ) {
    int secondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
      arrivalTime,
      arrivalTime,
      transitService.getTimeZone()
    );
    int secondsSinceMidnightYesterday = ServiceDateUtils.secondsSinceStartOfService(
      arrivalTime.minusDays(1),
      arrivalTime,
      transitService.getTimeZone()
    );

    String lastStopId = lastStop.getId().getId();
    Set<Trip> trips = cache.tripsByLastStopArrival(lastStopId, secondsSinceMidnight);
    if (trips.isEmpty()) {
      //Attempt to fetch trips that started yesterday - i.e. add 24 hours to arrival-time
      trips = cache.tripsByLastStopArrival(lastStopId, secondsSinceMidnightYesterday);
    }

    if (!trips.isEmpty()) {
      return trips;
    }

    //SIRI-data may report other platform, but still on the same Parent-stop
    if (!lastStop.isPartOfStation()) {
      return Set.of();
    }

    Set<Trip> tripsOnSiblingQuays = new HashSet<>();
    var allQuays = lastStop.getParentStation().getChildStops();
    for (var quay : allQuays) {
      tripsOnSiblingQuays.addAll(
        cache.tripsByLastStopArrival(quay.getId().getId(), secondsSinceMidnight)
      );
    }
    return tripsOnSiblingQuays;
  }

  /**
   * Finds the correct trip based on OTP-ServiceDate and SIRI-DepartureTime
   */
  private TripAndPattern getTripAndPatternForJourney(
    Set<Trip> trips,
    List<CallWrapper> calls,
    EntityResolver entityResolver,
    BiFunction<TripPattern, LocalDate, Timetable> getCurrentTimetable,
    BiFunction<FeedScopedId, LocalDate, TripPattern> getNewTripPatternForModifiedTrip
  ) throws UpdateException {
    var journeyFirstStop = entityResolver.resolveQuay(calls.getFirst().getStopPointRef());
    var journeyLastStop = entityResolver.resolveQuay(calls.getLast().getStopPointRef());
    if (journeyFirstStop == null || journeyLastStop == null) {
      throw UpdateException.of(NO_VALID_STOPS);
    }

    ZonedDateTime date = calls.getFirst().getAimedDepartureTime();
    LocalDate serviceDate = date.toLocalDate();

    int departureInSecondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
      date,
      date,
      transitService.getTimeZone()
    );
    TripCalendars calendarService = transitService.getTripCalendars();
    Set<TripAndPattern> possibleTrips = new HashSet<>();
    for (Trip trip : trips) {
      if (!calendarService.listServiceDates(trip.getServiceId()).contains(serviceDate)) {
        continue;
      }

      var newTripPatternForModifiedTrip = getNewTripPatternForModifiedTrip.apply(
        trip.getId(),
        serviceDate
      );
      TripPattern tripPattern = newTripPatternForModifiedTrip != null
        ? newTripPatternForModifiedTrip
        : transitService.findPattern(trip);

      var firstStop = tripPattern.firstStop();
      var lastStop = tripPattern.lastStop();

      boolean firstStopIsMatch =
        firstStop.equals(journeyFirstStop) || firstStop.isPartOfSameStationAs(journeyFirstStop);
      boolean lastStopIsMatch =
        lastStop.equals(journeyLastStop) || lastStop.isPartOfSameStationAs(journeyLastStop);

      if (!firstStopIsMatch || !lastStopIsMatch) {
        continue;
      }

      TripTimes times = getCurrentTimetable.apply(tripPattern, serviceDate).getTripTimes(trip);
      if (times != null && times.getScheduledDepartureTime(0) == departureInSecondsSinceMidnight) {
        // Found matches
        possibleTrips.add(new TripAndPattern(times.getTrip(), tripPattern));
      }
    }

    if (possibleTrips.isEmpty()) {
      throw UpdateException.of(UpdateErrorType.NO_FUZZY_TRIP_MATCH);
    } else if (possibleTrips.size() > 1) {
      throw UpdateException.of(UpdateErrorType.MULTIPLE_FUZZY_TRIP_MATCHES);
    } else {
      return possibleTrips.iterator().next();
    }
  }
}
