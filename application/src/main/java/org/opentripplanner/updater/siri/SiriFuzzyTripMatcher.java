package org.opentripplanner.updater.siri;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_VALID_STOPS;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.VehicleModesEnumeration;

/**
 * This class is used for matching TripDescriptors without trip_ids to scheduled GTFS data and to
 * feed back that information into a new TripDescriptor with proper trip_id.
 * <p>
 * It is mainly written for Entur (Norway) data, which doesn't yet have complete ID-based matching
 * of SIRI messages to transit model objects. But it may be easily adaptable to other locations, as
 * it's mainly looking at the last stop and arrival times of the scheduled trip. The matching
 * process will always be applied even in places where you have good quality IDs in SIRI data and
 * don't need it - we'd have to add a way to disable it.
 * <p>
 * The same instance of this SiriFuzzyTripMatcher may appear in different SIRI updaters. Be sure
 * to fetch the instance at during the setup of the updaters, the initialization is not thread-safe.
 */
public class SiriFuzzyTripMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);

  private final Map<String, Set<Trip>> internalPlanningCodeCache = new HashMap<>();
  private final Map<String, Set<Trip>> startStopTripCache = new HashMap<>();
  private final TransitService transitService;

  public SiriFuzzyTripMatcher(TransitService transitService) {
    this.transitService = transitService;
    initCache(this.transitService);
  }

  /**
   * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
   */
  public Result<TripAndPattern, UpdateError.UpdateErrorType> match(
    EstimatedVehicleJourney journey,
    EntityResolver entityResolver,
    BiFunction<TripPattern, LocalDate, Timetable> getCurrentTimetable,
    BiFunction<FeedScopedId, LocalDate, TripPattern> getNewTripPatternForModifiedTrip
  ) {
    List<CallWrapper> calls = CallWrapper.of(journey);

    if (calls.isEmpty()) {
      return Result.failure(NO_VALID_STOPS);
    }

    if (calls.getFirst().getAimedDepartureTime() == null) {
      return Result.failure(NO_FUZZY_TRIP_MATCH);
    }

    Set<Trip> trips = null;
    if (
      journey.getVehicleRef() != null &&
      journey.getVehicleModes().contains(VehicleModesEnumeration.RAIL)
    ) {
      trips = getCachedTripsByInternalPlanningCode(journey.getVehicleRef().getValue());
    }

    if (trips == null || trips.isEmpty()) {
      CallWrapper lastCall = calls.getLast();
      // resolves a scheduled stop point id to a quay (regular stop) if necessary
      // quay ids also work
      RegularStop stop = entityResolver.resolveQuay(lastCall.getStopPointRef());
      if (stop == null) {
        return Result.failure(NO_FUZZY_TRIP_MATCH);
      }
      ZonedDateTime arrivalTime = lastCall.getAimedArrivalTime() != null
        ? lastCall.getAimedArrivalTime()
        : lastCall.getAimedDepartureTime();

      if (arrivalTime != null) {
        trips = getMatchingTripsOnStopOrSiblings(stop, arrivalTime);
      }
    }
    if (trips == null || trips.isEmpty()) {
      return Result.failure(NO_FUZZY_TRIP_MATCH);
    }

    if (journey.getLineRef() != null) {
      var lineRef = journey.getLineRef().getValue();
      Route route = entityResolver.resolveRoute(lineRef);
      if (route != null) {
        trips =
          trips.stream().filter(trip -> trip.getRoute().equals(route)).collect(Collectors.toSet());
      }
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
    for (Trip trip : getCachedTripsByInternalPlanningCode(internalPlanningCode)) {
      Set<LocalDate> serviceDates = transitService
        .getCalendarService()
        .getServiceDatesForServiceId(trip.getServiceId());
      if (serviceDates.contains(serviceDate)) {
        matches.add(trip.getId());
      }
    }

    return matches;
  }

  private void initCache(TransitService index) {
    for (Trip trip : index.listTrips()) {
      TripPattern tripPattern = index.findPattern(trip);

      if (tripPattern == null) {
        continue;
      }

      if (tripPattern.getRoute().getMode().equals(TransitMode.RAIL)) {
        String internalPlanningCode = trip.getNetexInternalPlanningCode();
        if (internalPlanningCode != null) {
          internalPlanningCodeCache
            .computeIfAbsent(internalPlanningCode, key -> new HashSet<>())
            .add(trip);
        }
      }
      String lastStopId = tripPattern.lastStop().getId().getId();

      TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
      if (tripTimes != null) {
        int arrivalTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);

        String key = createStartStopKey(lastStopId, arrivalTime);
        startStopTripCache.computeIfAbsent(key, k -> new HashSet<>()).add(trip);
      }
    }

    LOG.info("Built internalPlanningCode-cache [{}].", internalPlanningCodeCache.size());
    LOG.info("Built start-stop-cache [{}].", startStopTripCache.size());
  }

  private static String createStartStopKey(RegularStop stop, int lastStopArrivalTime) {
    return createStartStopKey(stop.getId().getId(), lastStopArrivalTime);
  }

  private static String createStartStopKey(String lastStopId, int lastStopArrivalTime) {
    return lastStopId + ":" + lastStopArrivalTime;
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

    Set<Trip> trips = startStopTripCache.get(createStartStopKey(lastStop, secondsSinceMidnight));
    if (trips == null) {
      //Attempt to fetch trips that started yesterday - i.e. add 24 hours to arrival-time
      trips = startStopTripCache.get(createStartStopKey(lastStop, secondsSinceMidnightYesterday));
    }

    if (trips != null) {
      return trips;
    }

    //SIRI-data may report other platform, but still on the same Parent-stop
    if (!lastStop.isPartOfStation()) {
      return Set.of();
    }

    trips = new HashSet<>();
    var allQuays = lastStop.getParentStation().getChildStops();
    for (var quay : allQuays) {
      Set<Trip> tripSet = startStopTripCache.get(
        createStartStopKey(quay.getId().getId(), secondsSinceMidnight)
      );
      if (tripSet != null) {
        trips.addAll(tripSet);
      }
    }
    return trips;
  }

  private Set<Trip> getCachedTripsByInternalPlanningCode(String internalPlanningCode) {
    if (internalPlanningCode == null) {
      return null;
    }
    return internalPlanningCodeCache.getOrDefault(internalPlanningCode, new HashSet<>());
  }

  /**
   * Finds the correct trip based on OTP-ServiceDate and SIRI-DepartureTime
   */
  private Result<TripAndPattern, UpdateError.UpdateErrorType> getTripAndPatternForJourney(
    Set<Trip> trips,
    List<CallWrapper> calls,
    EntityResolver entityResolver,
    BiFunction<TripPattern, LocalDate, Timetable> getCurrentTimetable,
    BiFunction<FeedScopedId, LocalDate, TripPattern> getNewTripPatternForModifiedTrip
  ) {
    var journeyFirstStop = entityResolver.resolveQuay(calls.getFirst().getStopPointRef());
    var journeyLastStop = entityResolver.resolveQuay(calls.getLast().getStopPointRef());
    if (journeyFirstStop == null || journeyLastStop == null) {
      return Result.failure(NO_VALID_STOPS);
    }

    ZonedDateTime date = calls.getFirst().getAimedDepartureTime();
    LocalDate serviceDate = date.toLocalDate();

    int departureInSecondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
      date,
      date,
      transitService.getTimeZone()
    );
    CalendarService calendarService = transitService.getCalendarService();
    Set<TripAndPattern> possibleTrips = new HashSet<>();
    for (Trip trip : trips) {
      if (!calendarService.getServiceDatesForServiceId(trip.getServiceId()).contains(serviceDate)) {
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
      return Result.failure(UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH);
    } else if (possibleTrips.size() > 1) {
      LOG.warn("Multiple trip and pattern combinations found, skipping all, {}", possibleTrips);
      return Result.failure(UpdateError.UpdateErrorType.MULTIPLE_FUZZY_TRIP_MATCHES);
    } else {
      return Result.success(possibleTrips.iterator().next());
    }
  }
}
