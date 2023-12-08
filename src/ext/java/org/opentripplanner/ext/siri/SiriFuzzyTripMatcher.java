package org.opentripplanner.ext.siri;

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
import javax.annotation.Nonnull;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
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

  private static SiriFuzzyTripMatcher instance;

  private final Map<String, Set<Trip>> internalPlanningCodeCache = new HashMap<>();
  private final Map<String, Set<Trip>> startStopTripCache = new HashMap<>();
  private final TransitService transitService;
  private boolean initialized = false;

  /**
   * Factory method used to create only one instance.
   * <p>
   * THIS METHOD IS NOT THREAD-SAFE AND SHOULD BE CALLED DURING THE
   * INITIALIZATION PROCESS.
   */
  public static SiriFuzzyTripMatcher of(TransitService transitService) {
    if (instance == null) {
      instance = new SiriFuzzyTripMatcher(transitService);
    }
    return instance;
  }

  private SiriFuzzyTripMatcher(TransitService transitService) {
    this.transitService = transitService;
    initCache(this.transitService);
  }

  /**
   * Matches VehicleActivity to a set of possible Trips based on tripId
   */
  public Trip match(
    MonitoredVehicleJourneyStructure monitoredVehicleJourney,
    EntityResolver entityResolver
  ) {
    if (monitoredVehicleJourney.getDestinationRef() != null) {
      String destinationRef = monitoredVehicleJourney.getDestinationRef().getValue();
      ZonedDateTime arrivalTime = monitoredVehicleJourney.getDestinationAimedArrivalTime();

      if (arrivalTime != null) {
        Set<Trip> trips = getMatchingTripsOnStopOrSiblings(
          destinationRef,
          arrivalTime,
          entityResolver
        );
        if (trips.isEmpty()) {
          return null;
        }
        return getTripForJourney(trips, monitoredVehicleJourney);
      }
    }
    return null;
  }

  /**
   * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
   */
  public TripAndPattern match(
    EstimatedVehicleJourney journey,
    EntityResolver entityResolver,
    BiFunction<TripPattern, LocalDate, Timetable> getCurrentTimetable,
    BiFunction<FeedScopedId, LocalDate, TripPattern> getRealtimeAddedTripPattern
  ) {
    List<CallWrapper> calls = CallWrapper.of(journey);

    if (calls.isEmpty()) {
      return null;
    }

    Set<Trip> trips = null;
    if (
      journey.getVehicleRef() != null &&
      journey.getVehicleModes().contains(VehicleModesEnumeration.RAIL)
    ) {
      trips = getCachedTripsByInternalPlanningCode(journey.getVehicleRef().getValue());
    }

    if (trips == null || trips.isEmpty()) {
      CallWrapper lastStop = calls.get(calls.size() - 1);
      String lastStopPoint = lastStop.getStopPointRef();
      ZonedDateTime arrivalTime = lastStop.getAimedArrivalTime() != null
        ? lastStop.getAimedArrivalTime()
        : lastStop.getAimedDepartureTime();

      if (arrivalTime != null) {
        trips = getMatchingTripsOnStopOrSiblings(lastStopPoint, arrivalTime, entityResolver);
      }
    }
    if (trips == null || trips.isEmpty()) {
      return null;
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
      getRealtimeAddedTripPattern
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
    if (!initialized) {
      for (Trip trip : index.getAllTrips()) {
        TripPattern tripPattern = index.getPatternForTrip(trip);

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

    initialized = true;
  }

  private static String createStartStopKey(String lastStopId, int lastStopArrivalTime) {
    return lastStopId + ":" + lastStopArrivalTime;
  }

  @Nonnull
  private Set<Trip> getMatchingTripsOnStopOrSiblings(
    String lastStopPoint,
    ZonedDateTime arrivalTime,
    EntityResolver entityResolver
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

    Set<Trip> trips = startStopTripCache.get(
      createStartStopKey(lastStopPoint, secondsSinceMidnight)
    );
    if (trips == null) {
      //Attempt to fetch trips that started yesterday - i.e. add 24 hours to arrival-time
      trips =
        startStopTripCache.get(createStartStopKey(lastStopPoint, secondsSinceMidnightYesterday));
    }

    if (trips != null) {
      return trips;
    }

    //SIRI-data may report other platform, but still on the same Parent-stop
    var stop = entityResolver.resolveQuay(lastStopPoint);
    if (stop == null || !stop.isPartOfStation()) {
      return Set.of();
    }

    trips = new HashSet<>();
    var allQuays = stop.getParentStation().getChildStops();
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
  private TripAndPattern getTripAndPatternForJourney(
    Set<Trip> trips,
    List<CallWrapper> calls,
    EntityResolver entityResolver,
    BiFunction<TripPattern, LocalDate, Timetable> getCurrentTimetable,
    BiFunction<FeedScopedId, LocalDate, TripPattern> getRealtimeAddedTripPattern
  ) {
    var journeyFirstStop = entityResolver.resolveQuay(calls.get(0).getStopPointRef());
    var journeyLastStop = entityResolver.resolveQuay(calls.get(calls.size() - 1).getStopPointRef());
    if (journeyFirstStop == null || journeyLastStop == null) {
      return null;
    }

    ZonedDateTime date = calls.get(0).getAimedDepartureTime();
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

      var realTimeAddedTripPattern = getRealtimeAddedTripPattern.apply(trip.getId(), serviceDate);
      TripPattern tripPattern = realTimeAddedTripPattern != null
        ? realTimeAddedTripPattern
        : transitService.getPatternForTrip(trip);

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
      return null;
    } else if (possibleTrips.size() > 1) {
      LOG.warn("Multiple trip and pattern combinations found, skipping all, {}", possibleTrips);
      return null;
    } else {
      return possibleTrips.iterator().next();
    }
  }

  /**
   * Finds the correct trip based on OTP-ServiceDate and SIRI-DepartureTime
   */
  private Trip getTripForJourney(
    Set<Trip> trips,
    MonitoredVehicleJourneyStructure monitoredVehicleJourney
  ) {
    ZonedDateTime date = monitoredVehicleJourney.getOriginAimedDepartureTime();
    if (date == null) {
      //If no date is set - assume Realtime-data is reported for 'today'.
      date = ZonedDateTime.now();
    }
    LocalDate serviceDate = date.toLocalDate();

    List<Trip> results = new ArrayList<>();
    for (Trip trip : trips) {
      Set<LocalDate> serviceDatesForServiceId = transitService
        .getCalendarService()
        .getServiceDatesForServiceId(trip.getServiceId());

      for (LocalDate next : serviceDatesForServiceId) {
        if (next.equals(serviceDate)) {
          results.add(trip);
        }
      }
    }

    if (results.size() == 1) {
      return results.get(0);
    } else if (results.size() > 1) {
      // Multiple possible matches - check if lineRef/routeId matches
      if (
        monitoredVehicleJourney.getLineRef() != null &&
        monitoredVehicleJourney.getLineRef().getValue() != null
      ) {
        String lineRef = monitoredVehicleJourney.getLineRef().getValue();
        for (Trip trip : results) {
          if (lineRef.equals(trip.getRoute().getId().getId())) {
            // Return first trip where the lineRef matches routeId
            return trip;
          }
        }
      }

      // Line does not match any routeId - return first result.
      return results.get(0);
    }

    return null;
  }
}
