package org.opentripplanner.ext.siri;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
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
  public Set<Trip> match(MonitoredVehicleJourneyStructure monitoredVehicleJourney, String feedId) {
    if (monitoredVehicleJourney != null) {
      if (monitoredVehicleJourney.getFramedVehicleJourneyRef() != null) {
        String datedVehicleRef = monitoredVehicleJourney
          .getFramedVehicleJourneyRef()
          .getDatedVehicleJourneyRef();
        if (datedVehicleRef != null) {
          Trip trip = transitService.getTripForId(new FeedScopedId(feedId, datedVehicleRef));
          if (trip != null) {
            return Set.of(trip);
          }
        }
      }
      if (monitoredVehicleJourney.getDestinationRef() != null) {
        String destinationRef = monitoredVehicleJourney.getDestinationRef().getValue();
        ZonedDateTime arrivalTime = monitoredVehicleJourney.getDestinationAimedArrivalTime();

        if (arrivalTime != null) {
          return getMatchingTripsOnStopOrSiblings(destinationRef, feedId, arrivalTime);
        }
      }
    }
    return Set.of();
  }

  /**
   * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
   */
  public Set<Trip> match(EstimatedVehicleJourney journey, String feedId) {
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
      String serviceJourneyId = resolveDatedVehicleJourneyRef(journey);
      if (serviceJourneyId != null) {
        Trip trip = transitService.getTripForId(new FeedScopedId(feedId, serviceJourneyId));
        if (trip != null) {
          trips = Set.of(trip);
        }
      }
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
        trips = getMatchingTripsOnStopOrSiblings(lastStopPoint, feedId, arrivalTime);
      }
    }
    return trips;
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

  static Trip findTripByDatedVehicleJourneyRef(
    EstimatedVehicleJourney journey,
    String feedId,
    TransitService transitService
  ) {
    String serviceJourneyId = resolveDatedVehicleJourneyRef(journey);
    if (serviceJourneyId != null) {
      Trip trip = transitService.getTripForId(new FeedScopedId(feedId, serviceJourneyId));
      if (trip != null) {
        return trip;
      } else {
        //Attempt to find trip using datedServiceJourneyId
        TripOnServiceDate tripOnServiceDate = transitService.getTripOnServiceDateById(
          new FeedScopedId(feedId, serviceJourneyId)
        );
        if (tripOnServiceDate != null) {
          return tripOnServiceDate.getTrip();
        }
      }
    }
    return null;
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

  private static String resolveDatedVehicleJourneyRef(EstimatedVehicleJourney journey) {
    if (journey.getFramedVehicleJourneyRef() != null) {
      return journey.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef();
    } else if (journey.getDatedVehicleJourneyRef() != null) {
      return journey.getDatedVehicleJourneyRef().getValue();
    }

    return null;
  }

  private Set<Trip> getMatchingTripsOnStopOrSiblings(
    String lastStopPoint,
    String feedId,
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
      var stop = transitService.getRegularStop(new FeedScopedId(feedId, lastStopPoint));
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
}
