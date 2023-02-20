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
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri20.RecordedCall;
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
        if (trips == null || trips.isEmpty()) {
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
    Set<Trip> trips = null;
    if (
      journey.getVehicleRef() != null &&
      (
        journey.getVehicleModes() != null &&
        journey.getVehicleModes().contains(VehicleModesEnumeration.RAIL)
      )
    ) {
      trips = getCachedTripsByInternalPlanningCode(journey.getVehicleRef().getValue());
    }

    if (trips == null || trips.isEmpty()) {
      String lastStopPoint = null;
      ZonedDateTime arrivalTime = null;

      if (
        journey.getEstimatedCalls() != null &&
        journey.getEstimatedCalls().getEstimatedCalls() != null &&
        !journey.getEstimatedCalls().getEstimatedCalls().isEmpty()
      ) { // Pick last stop from EstimatedCalls
        List<EstimatedCall> estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();
        EstimatedCall lastStop = estimatedCalls.get(estimatedCalls.size() - 1);

        lastStopPoint = lastStop.getStopPointRef().getValue();
        arrivalTime =
          lastStop.getAimedArrivalTime() != null
            ? lastStop.getAimedArrivalTime()
            : lastStop.getAimedDepartureTime();
      } else if (
        journey.getRecordedCalls() != null &&
        journey.getRecordedCalls().getRecordedCalls() != null &&
        !journey.getRecordedCalls().getRecordedCalls().isEmpty()
      ) { // No EstimatedCalls exist - pick last RecordedCall
        List<RecordedCall> recordedCalls = journey.getRecordedCalls().getRecordedCalls();
        final RecordedCall lastStop = recordedCalls.get(recordedCalls.size() - 1);

        lastStopPoint = lastStop.getStopPointRef().getValue();
        arrivalTime =
          lastStop.getAimedArrivalTime() != null
            ? lastStop.getAimedArrivalTime()
            : lastStop.getAimedDepartureTime();
      }

      if (arrivalTime != null) {
        trips = getMatchingTripsOnStopOrSiblings(lastStopPoint, arrivalTime, entityResolver);
      }
    }
    if (trips == null || trips.isEmpty()) {
      return null;
    }

    trips = getTripForJourney(trips, journey, getCurrentTimetable);
    if (trips == null || trips.isEmpty()) {
      return null;
    }

    var result = new ArrayList<TripAndPattern>();

    for (var trip : trips) {
      var pattern = getPatternForTrip(trip, journey, getRealtimeAddedTripPattern);
      if (pattern != null) {
        result.add(new TripAndPattern(trip, pattern));
      }
    }

    if (result.isEmpty()) {
      return null;
    } else if (result.size() > 1) {
      LOG.warn("Multiple trip and pattern combinations found, skipping all, {}", result);
      return null;
    } else {
      return result.get(0);
    }
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

    if (trips == null || trips.isEmpty()) {
      //SIRI-data may report other platform, but still on the same Parent-stop
      var stop = entityResolver.resolveQuay(lastStopPoint);
      if (stop != null && stop.isPartOfStation()) {
        // TODO OTP2 resolve stop-station split
        var allQuays = stop.getParentStation().getChildStops();
        for (var quay : allQuays) {
          Set<Trip> tripSet = startStopTripCache.get(
            createStartStopKey(quay.getId().getId(), secondsSinceMidnight)
          );
          if (tripSet != null) {
            if (trips == null) {
              trips = tripSet;
            } else {
              trips.addAll(tripSet);
            }
          }
        }
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
  private Set<Trip> getTripForJourney(
    Set<Trip> trips,
    EstimatedVehicleJourney journey,
    BiFunction<TripPattern, LocalDate, Timetable> getCurrentTimetable
  ) {
    List<RecordedCall> recordedCalls =
      (
        journey.getRecordedCalls() != null
          ? journey.getRecordedCalls().getRecordedCalls()
          : new ArrayList<>()
      );
    List<EstimatedCall> estimatedCalls =
      (
        journey.getEstimatedCalls() != null ? journey.getEstimatedCalls().getEstimatedCalls() : null
      );

    ZonedDateTime date;
    int stopNumber = 1;
    String firstStopId;
    if (recordedCalls != null && !recordedCalls.isEmpty()) {
      RecordedCall recordedCall = recordedCalls.get(0);
      date = recordedCall.getAimedDepartureTime();
      firstStopId = recordedCall.getStopPointRef().getValue();
    } else if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
      EstimatedCall estimatedCall = estimatedCalls.get(0);
      if (estimatedCall.getOrder() != null) {
        stopNumber = estimatedCall.getOrder().intValue();
      } else if (estimatedCall.getVisitNumber() != null) {
        stopNumber = estimatedCall.getVisitNumber().intValue();
      }
      firstStopId = estimatedCall.getStopPointRef().getValue();
      date = estimatedCall.getAimedDepartureTime();
    } else {
      return null;
    }

    if (date == null) {
      //If no date is set - assume Realtime-data is reported for 'today'.
      date = ZonedDateTime.now(transitService.getTimeZone());
    }
    LocalDate serviceDate = date.toLocalDate();

    int departureInSecondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
      date,
      date,
      transitService.getTimeZone()
    );
    Set<Trip> result = new HashSet<>();
    for (Trip trip : trips) {
      Set<LocalDate> serviceDatesForServiceId = transitService
        .getCalendarService()
        .getServiceDatesForServiceId(trip.getServiceId());
      if (serviceDatesForServiceId.contains(serviceDate)) {
        TripPattern pattern = transitService.getPatternForTrip(trip);

        if (stopNumber < pattern.numberOfStops()) {
          boolean firstReportedStopIsFound = false;
          var stop = pattern.getStop(stopNumber - 1);
          if (firstStopId.equals(stop.getId().getId())) {
            firstReportedStopIsFound = true;
          } else {
            if (stop.isPartOfStation()) {
              var alternativeStop = transitService.getRegularStop(
                new FeedScopedId(stop.getId().getFeedId(), firstStopId)
              );
              if (alternativeStop != null && stop.isPartOfSameStationAs(alternativeStop)) {
                firstReportedStopIsFound = true;
              }
            }
          }
          if (firstReportedStopIsFound) {
            for (TripTimes times : getCurrentTimetable.apply(pattern, serviceDate).getTripTimes()) {
              if (
                times.getScheduledDepartureTime(stopNumber - 1) == departureInSecondsSinceMidnight
              ) {
                if (
                  transitService
                    .getCalendarService()
                    .getServiceDatesForServiceId(times.getTrip().getServiceId())
                    .contains(serviceDate)
                ) {
                  result.add(times.getTrip());
                }
              }
            }
          }
        }
      }
    }

    if (result.size() >= 1) {
      return result;
    } else {
      return null;
    }
  }

  private TripPattern getPatternForTrip(
    Trip trip,
    EstimatedVehicleJourney journey,
    BiFunction<FeedScopedId, LocalDate, TripPattern> getRealtimeAddedTripPattern
  ) {
    Set<LocalDate> serviceDates = transitService
      .getCalendarService()
      .getServiceDatesForServiceId(trip.getServiceId());

    List<RecordedCall> recordedCalls =
      (
        journey.getRecordedCalls() != null
          ? journey.getRecordedCalls().getRecordedCalls()
          : new ArrayList<>()
      );
    List<EstimatedCall> estimatedCalls;
    if (journey.getEstimatedCalls() != null) {
      estimatedCalls = journey.getEstimatedCalls().getEstimatedCalls();
    } else {
      return null;
    }

    String journeyFirstStopId;
    String journeyLastStopId;
    LocalDate journeyDate;
    //Resolve first stop - check recordedCalls, then estimatedCalls
    if (recordedCalls != null && !recordedCalls.isEmpty()) {
      RecordedCall recordedCall = recordedCalls.get(0);
      journeyFirstStopId = recordedCall.getStopPointRef().getValue();
      journeyDate = recordedCall.getAimedDepartureTime().toLocalDate();
    } else if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
      EstimatedCall estimatedCall = estimatedCalls.get(0);
      journeyFirstStopId = estimatedCall.getStopPointRef().getValue();
      journeyDate = estimatedCall.getAimedDepartureTime().toLocalDate();
    } else {
      return null;
    }

    //Resolve last stop - check estimatedCalls, then recordedCalls
    if (estimatedCalls != null && !estimatedCalls.isEmpty()) {
      EstimatedCall estimatedCall = estimatedCalls.get(estimatedCalls.size() - 1);
      journeyLastStopId = estimatedCall.getStopPointRef().getValue();
    } else if (recordedCalls != null && !recordedCalls.isEmpty()) {
      RecordedCall recordedCall = recordedCalls.get(recordedCalls.size() - 1);
      journeyLastStopId = recordedCall.getStopPointRef().getValue();
    } else {
      return null;
    }

    TripPattern realtimeAddedTripPattern = null;
    if (getRealtimeAddedTripPattern != null) {
      realtimeAddedTripPattern = getRealtimeAddedTripPattern.apply(trip.getId(), journeyDate);
    }

    TripPattern tripPattern;
    if (realtimeAddedTripPattern != null) {
      tripPattern = realtimeAddedTripPattern;
    } else {
      tripPattern = transitService.getPatternForTrip(trip);
    }

    var firstStop = tripPattern.firstStop();
    var lastStop = tripPattern.lastStop();

    if (serviceDates.contains(journeyDate)) {
      boolean firstStopIsMatch = firstStop.getId().getId().equals(journeyFirstStopId);
      boolean lastStopIsMatch = lastStop.getId().getId().equals(journeyLastStopId);

      if (!firstStopIsMatch && firstStop.isPartOfStation()) {
        var otherFirstStop = transitService.getRegularStop(
          new FeedScopedId(firstStop.getId().getFeedId(), journeyFirstStopId)
        );
        firstStopIsMatch = firstStop.isPartOfSameStationAs(otherFirstStop);
      }

      if (!lastStopIsMatch && lastStop.isPartOfStation()) {
        var otherLastStop = transitService.getRegularStop(
          new FeedScopedId(lastStop.getId().getFeedId(), journeyLastStopId)
        );
        lastStopIsMatch = lastStop.isPartOfSameStationAs(otherLastStop);
      }

      if (firstStopIsMatch & lastStopIsMatch) {
        // Found matches
        return tripPattern;
      }

      return null;
    }

    return null;
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
