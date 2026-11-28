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
 * the times to be equal to the second. A trip that matches the aimed times to the second is
 * preferred over trips within the tolerance, even if it arrives at another quay of the same
 * station as the reported one. If there is no single best match, the journey is rejected with
 * {@link UpdateErrorType#MULTIPLE_FUZZY_TRIP_MATCHES}.
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
   * Matches a journey to a scheduled trip by the aimed arrival time at its last stop and the aimed
   * departure time at its first stop.
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

    CallWrapper lastCall = calls.getLast();
    // accepts a quay id or a scheduled stop point id
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

    // Try the candidate lookups in order of preference. The first lookup with an exact match is
    // used, so an exact match on a sibling quay wins over a trip within the tolerance at the
    // reported quay. Without an exact match, the first lookup with a candidate that passes all the
    // checks is used. With a time tolerance, a lookup may return unrelated trips, which must not
    // prevent the following lookups.
    List<Candidate> firstCandidates = List.of();
    for (var lookup : candidateLookups(stop, arrivalTime)) {
      Set<Trip> trips = filterByRoute(lookup.trips().get(), route);
      if (trips.isEmpty()) {
        continue;
      }
      var possibleTrips = findPossibleTrips(
        trips,
        calls,
        lookup.arrivalTime(),
        entityResolver,
        getCurrentTimetable,
        getNewTripPatternForModifiedTrip
      );
      if (possibleTrips.stream().anyMatch(Candidate::exact)) {
        return singleMatch(possibleTrips);
      }
      if (firstCandidates.isEmpty()) {
        firstCandidates = possibleTrips;
      }
    }
    return singleMatch(firstCandidates);
  }

  /**
   * Return the only exact match if there is one, otherwise the only possible trip. Reject the
   * journey if there is no possible trip, or more than one without exactly one exact match.
   */
  private static TripAndPattern singleMatch(List<Candidate> possibleTrips) throws UpdateException {
    var exactMatches = possibleTrips.stream().filter(Candidate::exact).toList();
    if (exactMatches.size() == 1) {
      return exactMatches.getFirst().tripAndPattern();
    }
    if (possibleTrips.isEmpty()) {
      throw UpdateException.of(NO_FUZZY_TRIP_MATCH);
    }
    if (possibleTrips.size() > 1) {
      throw UpdateException.of(UpdateErrorType.MULTIPLE_FUZZY_TRIP_MATCHES);
    }
    return possibleTrips.getFirst().tripAndPattern();
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
   * The candidate lookups for the last stop, in order of preference:
   * <ol>
   *   <li>trips arriving at the reported stop,</li>
   *   <li>trips that started yesterday and arrive at the reported stop, i.e. with 24 hours added
   *   to the arrival time,</li>
   *   <li>trips arriving at any quay of the same station, since SIRI data may report another
   *   platform.</li>
   * </ol>
   * The lookups are evaluated lazily, so a lookup is only done if the previous ones gave no exact
   * match.
   */
  private List<Lookup> candidateLookups(RegularStop lastStop, ZonedDateTime arrivalTime) {
    var timeZone = transitRepository.getTimeZone();
    int secondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
      arrivalTime,
      arrivalTime,
      timeZone
    );
    int secondsSinceYesterday = ServiceDateUtils.secondsSinceStartOfService(
      arrivalTime.minusDays(1),
      arrivalTime,
      timeZone
    );
    String lastStopId = lastStop.getId().getId();

    List<Lookup> lookups = new ArrayList<>();
    lookups.add(
      new Lookup(
        () -> tripsByLastStopArrival(lastStopId, secondsSinceMidnight),
        secondsSinceMidnight
      )
    );
    lookups.add(
      new Lookup(
        () -> tripsByLastStopArrival(lastStopId, secondsSinceYesterday),
        secondsSinceYesterday
      )
    );
    if (lastStop.isPartOfStation()) {
      Supplier<Set<Trip>> tripsOnSiblingQuays = () -> {
        Set<Trip> trips = new HashSet<>();
        for (var quay : lastStop.getParentStation().getChildStops()) {
          trips.addAll(tripsByLastStopArrival(quay.getId().getId(), secondsSinceMidnight));
        }
        return trips;
      };
      lookups.add(new Lookup(tripsOnSiblingQuays, secondsSinceMidnight));
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
   * empty list if no candidate matches.
   *
   * @param aimedArrivalTime The aimed arrival time at the last stop, in seconds since the start of
   *                         the service date of the candidates. The candidates already arrive
   *                         within the time tolerance of it. It is only used to find out if a
   *                         candidate matches exactly.
   */
  private List<Candidate> findPossibleTrips(
    Set<Trip> trips,
    List<CallWrapper> calls,
    int aimedArrivalTime,
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
    List<Candidate> possibleTrips = new ArrayList<>();
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
      if (times == null) {
        continue;
      }
      int scheduledDeparture = times.getScheduledDepartureTime(0);
      if (isWithinTimeTolerance(scheduledDeparture, departureInSecondsSinceMidnight)) {
        boolean exact =
          scheduledDeparture == departureInSecondsSinceMidnight &&
          times.getScheduledArrivalTime(times.getNumStops() - 1) == aimedArrivalTime;
        possibleTrips.add(new Candidate(new TripAndPattern(times.getTrip(), tripPattern), exact));
      }
    }

    return possibleTrips;
  }

  /**
   * A lookup of candidate trips by their arrival time at the last stop.
   *
   * @param arrivalTime The aimed arrival time used for the lookup, in seconds since the start of
   *                    the service date of the candidates.
   */
  private record Lookup(Supplier<Set<Trip>> trips, int arrivalTime) {}

  /**
   * A trip that matches within the time tolerance.
   *
   * @param exact {@code true} if the scheduled times are equal to the aimed times, to the second.
   */
  private record Candidate(TripAndPattern tripAndPattern, boolean exact) {}
}
