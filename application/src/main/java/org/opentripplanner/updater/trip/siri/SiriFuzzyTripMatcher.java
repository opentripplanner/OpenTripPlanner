package org.opentripplanner.updater.trip.siri;

import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.updater.spi.UpdateErrorType.NO_VALID_STOPS;
import static org.opentripplanner.updater.spi.UpdateErrorType.UNKNOWN_STOP;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.service.TransitRepository;
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
 * The aimed arrival time at the last stop and the aimed departure time at the first stop must
 * match the scheduled times within the configured time tolerance. A tolerance of zero requires
 * the times to be equal to the second. If more than one trip matches, the journey is rejected
 * with {@link UpdateErrorType#MULTIPLE_FUZZY_TRIP_MATCHES}.
 * <p>
 * Constructed per update task (cheap — only assigns fields). The expensive scheduled-data index
 * lives in {@link SiriFuzzyTripMatcherCache}, which is built once and shared across all SIRI
 * updaters.
 */
public class SiriFuzzyTripMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);

  private final SiriFuzzyTripMatcherCache cache;
  private final TimetableRepository timetableRepository;
  private final TransitRepository transitRepository;

  /** The maximum difference between a scheduled and an aimed time, in seconds. */
  private final int timeToleranceSeconds;

  /**
   * @param timeTolerance The maximum difference between the scheduled time and the SIRI aimed
   *                      time for a trip to match, see {@link SiriFuzzyTripMatchingParameters}.
   */
  public SiriFuzzyTripMatcher(
    SiriFuzzyTripMatcherCache cache,
    TimetableRepository timetableRepository,
    TransitRepository transitRepository,
    Duration timeTolerance
  ) {
    this.cache = cache;
    this.timetableRepository = timetableRepository;
    this.transitRepository = transitRepository;
    this.timeToleranceSeconds = (int) timeTolerance.toSeconds();
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

    Optional<Route> route = journeyWrapper.lineRef().map(entityResolver::resolveRoute);

    if (journeyWrapper.vehicleRef().isPresent() && journeyWrapper.isRail()) {
      Set<Trip> trips = cache.tripsByInternalPlanningCode(journeyWrapper.vehicleRef().get());
      if (!trips.isEmpty()) {
        return singleMatch(
          findPossibleTrips(
            filterByRoute(trips, route),
            calls,
            entityResolver,
            getCurrentTimetable,
            getNewTripPatternForModifiedTrip
          )
        );
      }
    }

    CallWrapper lastCall = calls.getLast();
    // resolves a scheduled stop point id to a quay (regular stop) if necessary
    // quay ids also work
    RegularStop stop = entityResolver.resolveQuay(lastCall.getStopPointRef());
    if (stop == null) {
      throw UpdateException.of(UNKNOWN_STOP);
    }
    ZonedDateTime arrivalTime =
      lastCall.getAimedArrivalTime() != null
        ? lastCall.getAimedArrivalTime()
        : lastCall.getAimedDepartureTime();
    if (arrivalTime == null) {
      throw UpdateException.of(NO_FUZZY_TRIP_MATCH);
    }

    // Try the candidate lookups in order of preference. A lookup is only used if one of its
    // candidates passes all the checks, otherwise the next lookup is tried. With a time tolerance,
    // a lookup may return unrelated trips, which must not prevent the following lookups.
    for (var candidates : candidateLookups(stop, arrivalTime)) {
      Set<Trip> trips = filterByRoute(candidates.get(), route);
      if (trips.isEmpty()) {
        continue;
      }
      var possibleTrips = findPossibleTrips(
        trips,
        calls,
        entityResolver,
        getCurrentTimetable,
        getNewTripPatternForModifiedTrip
      );
      if (!possibleTrips.isEmpty()) {
        return singleMatch(possibleTrips);
      }
    }
    throw UpdateException.of(NO_FUZZY_TRIP_MATCH);
  }

  /**
   * Return the only possible trip, or reject the journey if there is none or more than one.
   */
  private static TripAndPattern singleMatch(Set<TripAndPattern> possibleTrips)
    throws UpdateException {
    if (possibleTrips.isEmpty()) {
      throw UpdateException.of(NO_FUZZY_TRIP_MATCH);
    }
    if (possibleTrips.size() > 1) {
      throw UpdateException.of(UpdateErrorType.MULTIPLE_FUZZY_TRIP_MATCHES);
    }
    return possibleTrips.iterator().next();
  }

  private static Set<Trip> filterByRoute(Set<Trip> trips, Optional<Route> route) {
    if (route.isEmpty()) {
      return trips;
    }
    return trips
      .stream()
      .filter(trip -> trip.getRoute().equals(route.get()))
      .collect(Collectors.toSet());
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
      Set<LocalDate> serviceDates = timetableRepository
        .getTripCalendars()
        .listServiceDates(trip.getServiceId());
      if (serviceDates.contains(serviceDate)) {
        matches.add(trip.getId());
      }
    }

    return matches;
  }

  /**
   * The candidate lookups for the last stop, in order of preference:
   * <ol>
   *   <li>trips arriving at the reported stop,</li>
   *   <li>trips that started yesterday and arrive at the reported stop, i.e. with 24 hours added
   *   to the arrival time,</li>
   *   <li>trips arriving at any quay of the same station, since SIRI data may report another
   *   platform.</li>
   * </ol>
   * The lookups are evaluated lazily, so a lookup is only done if the previous ones gave no match.
   */
  private List<Supplier<Set<Trip>>> candidateLookups(
    RegularStop lastStop,
    ZonedDateTime arrivalTime
  ) {
    var timeZone = transitRepository.getTimeZone();
    int secondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
      arrivalTime,
      arrivalTime,
      timeZone
    );
    String lastStopId = lastStop.getId().getId();

    List<Supplier<Set<Trip>>> lookups = new ArrayList<>();
    lookups.add(() -> tripsByLastStopArrival(lastStopId, secondsSinceMidnight));
    lookups.add(() ->
      tripsByLastStopArrival(
        lastStopId,
        ServiceDateUtils.secondsSinceStartOfService(arrivalTime.minusDays(1), arrivalTime, timeZone)
      )
    );
    if (lastStop.isPartOfStation()) {
      lookups.add(() -> {
        Set<Trip> tripsOnSiblingQuays = new HashSet<>();
        for (var quay : lastStop.getParentStation().getChildStops()) {
          tripsOnSiblingQuays.addAll(
            tripsByLastStopArrival(quay.getId().getId(), secondsSinceMidnight)
          );
        }
        return tripsOnSiblingQuays;
      });
    }
    return lookups;
  }

  /** The trips arriving at the given last stop within the time tolerance of the arrival time. */
  private Set<Trip> tripsByLastStopArrival(String stopId, int arrivalTime) {
    return cache.tripsByLastStopArrival(
      stopId,
      arrivalTime - timeToleranceSeconds,
      arrivalTime + timeToleranceSeconds
    );
  }

  private boolean isWithinTimeTolerance(int scheduledTime, int aimedTime) {
    return Math.abs(scheduledTime - aimedTime) <= timeToleranceSeconds;
  }

  /**
   * The candidate trips that run on the journey's service date, have matching first and last
   * stops, and depart within the time tolerance of the journey's aimed departure time. Returns an
   * empty set if no candidate matches.
   */
  private Set<TripAndPattern> findPossibleTrips(
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
      transitRepository.getTimeZone()
    );
    TripCalendars calendarService = timetableRepository.getTripCalendars();
    Set<TripAndPattern> possibleTrips = new HashSet<>();
    for (Trip trip : trips) {
      if (!calendarService.listServiceDates(trip.getServiceId()).contains(serviceDate)) {
        continue;
      }

      var newTripPatternForModifiedTrip = getNewTripPatternForModifiedTrip.apply(
        trip.getId(),
        serviceDate
      );
      TripPattern tripPattern =
        newTripPatternForModifiedTrip != null
          ? newTripPatternForModifiedTrip
          : timetableRepository.findPattern(trip);

      var firstStop = tripPattern.firstStop();
      var lastStop = tripPattern.lastStop();

      boolean firstStopIsMatch =
        firstStop.equals(journeyFirstStop) || firstStop.isPartOfSameStationAs(journeyFirstStop);
      boolean lastStopIsMatch =
        lastStop.equals(journeyLastStop) || lastStop.isPartOfSameStationAs(journeyLastStop);

      if (!firstStopIsMatch || !lastStopIsMatch) {
        continue;
      }

      var times = getCurrentTimetable.apply(tripPattern, serviceDate).getTripTimes(trip);
      if (
        times != null &&
        isWithinTimeTolerance(times.getScheduledDepartureTime(0), departureInSecondsSinceMidnight)
      ) {
        // Found matches
        possibleTrips.add(new TripAndPattern(times.getTrip(), tripPattern));
      }
    }

    return possibleTrips;
  }
}
