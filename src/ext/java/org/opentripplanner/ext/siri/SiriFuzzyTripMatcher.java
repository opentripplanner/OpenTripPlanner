package org.opentripplanner.ext.siri;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.VehicleActivityStructure;
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
 * Several instances of this SiriFuzzyTripMatcher may appear in different SIRI updaters, but they
 * all share a common set of static Map fields. We generally don't advocate using static fields in
 * this way, but as part of a sandbox contribution we are maintaining this implementation.
 */
public class SiriFuzzyTripMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcher.class);
  private static final Map<String, Set<Trip>> mappedTripsCache = new HashMap<>();
  private static final Map<String, Set<Trip>> mappedVehicleRefCache = new HashMap<>();
  private static final Map<String, Set<Route>> mappedRoutesCache = new HashMap<>();
  private static final Map<String, Set<Trip>> start_stop_tripCache = new HashMap<>();
  private static final Map<String, Trip> vehicleJourneyTripCache = new HashMap<>();
  private static final Set<String> nonExistingStops = new HashSet<>();

  private final TransitService transitService;

  public SiriFuzzyTripMatcher(TransitService transitService) {
    this.transitService = transitService;
    initCache(this.transitService);
  }

  /**
   * Matches VehicleActivity to a set of possible Trips based on tripId
   */
  public Set<Trip> match(VehicleActivityStructure activity) {
    VehicleActivityStructure.MonitoredVehicleJourney monitoredVehicleJourney = activity.getMonitoredVehicleJourney();
    Set<Trip> trips = new HashSet<>();
    if (monitoredVehicleJourney != null) {
      String datedVehicleRef = null;
      if (monitoredVehicleJourney.getFramedVehicleJourneyRef() != null) {
        datedVehicleRef =
          monitoredVehicleJourney.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef();
        if (datedVehicleRef != null) {
          trips = mappedTripsCache.get(datedVehicleRef);
        }
      }
      if (monitoredVehicleJourney.getDestinationRef() != null) {
        String destinationRef = monitoredVehicleJourney.getDestinationRef().getValue();
        ZonedDateTime arrivalTime = monitoredVehicleJourney.getDestinationAimedArrivalTime();

        if (arrivalTime != null) {
          trips = getMatchingTripsOnStopOrSiblings(destinationRef, arrivalTime);
        }
      }
    }

    return trips;
  }

  /**
   * Matches EstimatedVehicleJourney to a set of possible Trips based on tripId
   */
  public Set<Trip> match(EstimatedVehicleJourney journey) {
    Set<Trip> trips = null;
    if (
      journey.getVehicleRef() != null &&
      (
        journey.getVehicleModes() != null &&
        journey.getVehicleModes().contains(VehicleModesEnumeration.RAIL)
      )
    ) {
      trips = getCachedTripsByVehicleRef(journey.getVehicleRef().getValue());
    }

    if (trips == null || trips.isEmpty()) {
      String serviceJourneyId = resolveDatedVehicleJourneyRef(journey);
      if (serviceJourneyId != null) {
        trips = getCachedTripsBySiriId(serviceJourneyId);
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
        trips = getMatchingTripsOnStopOrSiblings(lastStopPoint, arrivalTime);
      }
    }
    return trips;
  }

  public Set<Route> getRoutesForStop(FeedScopedId siriStopId) {
    var stop = transitService.getStopForId(siriStopId);
    return transitService.getRoutesForStop(stop);
  }

  public FeedScopedId getStop(String siriStopId) {
    if (nonExistingStops.contains(siriStopId)) {
      return null;
    }

    // TODO OTP2 #2838 - Guessing on the feedId is not a deterministic way to find a stop.

    //First, assume same agency

    var firstStop = transitService.getAllStops().iterator().next();
    FeedScopedId id = new FeedScopedId(firstStop.getId().getFeedId(), siriStopId);
    if (transitService.getStopForId(id) != null) {
      return id;
    } else if (transitService.getStationById(id) != null) {
      return id;
    }

    //Not same agency - loop through all stops/Stations
    for (var stop : transitService.getAllStops()) {
      if (stop.getId().getId().equals(siriStopId)) {
        return stop.getId();
      }
    }
    //No match found in quays - check parent-stops (stopplace)
    for (Station station : transitService.getStations()) {
      if (station.getId().getId().equals(siriStopId)) {
        return station.getId();
      }
    }

    nonExistingStops.add(siriStopId);
    return null;
  }

  public Set<Route> getRoutes(String lineRefValue) {
    return mappedRoutesCache.getOrDefault(lineRefValue, new HashSet<>());
  }

  public FeedScopedId getTripId(String vehicleJourney, String feedId) {
    Trip trip = vehicleJourneyTripCache.get(vehicleJourney);
    if (trip != null) {
      return trip.getId();
    } else {
      //Attempt to find trip using datedServiceJourneys
      TripOnServiceDate tripOnServiceDate = transitService.getTripOnServiceDateById(
        new FeedScopedId(feedId, vehicleJourney)
      );
      if (tripOnServiceDate != null) {
        return tripOnServiceDate.getTrip().getId();
      }
    }
    //Fallback to handle extrajourneys
    trip = transitService.getTripForId(new FeedScopedId(feedId, vehicleJourney));
    if (trip != null) {
      vehicleJourneyTripCache.put(vehicleJourney, trip);
      return trip.getId();
    }
    return null;
  }

  public int getTripDepartureTime(FeedScopedId tripId) {
    Trip trip = transitService.getTripForId(tripId);
    {
      TripPattern tripPattern = transitService.getPatternForTrip(trip);

      if (tripPattern != null) {
        TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
        if (tripTimes != null) {
          return tripTimes.getArrivalTime(0);
        }
      }
    }
    return -1;
  }

  public int getTripArrivalTime(FeedScopedId tripId) {
    Trip trip = transitService.getTripForId(tripId);
    TripPattern tripPattern = transitService.getPatternForTrip(trip);

    if (tripPattern != null) {
      TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
      if (tripTimes != null) {
        return tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);
      }
    }
    return -1;
  }

  /**
   * Returns a match of tripIds that match the provided values.
   */
  public List<FeedScopedId> getTripIdForInternalPlanningCodeServiceDateAndMode(
    String internalPlanningCode,
    LocalDate serviceDate,
    TransitMode mode,
    SubMode transportSubmode
  ) {
    Set<Trip> cachedTripsBySiriId = getCachedTripsBySiriId(internalPlanningCode);

    if (cachedTripsBySiriId.isEmpty()) {
      cachedTripsBySiriId = getCachedTripsByVehicleRef(internalPlanningCode);
    }

    List<FeedScopedId> matches = new ArrayList<>();
    for (Trip trip : cachedTripsBySiriId) {
      final TripPattern tripPattern = transitService.getPatternForTrip(trip);
      if (tripPattern.matchesModeOrSubMode(mode, transportSubmode)) {
        Set<LocalDate> serviceDates = transitService
          .getCalendarService()
          .getServiceDatesForServiceId(trip.getServiceId());
        if (
          serviceDates.contains(serviceDate) &&
          trip.getNetexInternalPlanningCode() != null &&
          trip.getNetexInternalPlanningCode().equals(internalPlanningCode)
        ) {
          matches.add(trip.getId());
        }
      }
    }

    return matches;
  }

  Trip findTripByDatedVehicleJourneyRef(EstimatedVehicleJourney journey, String feedId) {
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

  private static void initCache(TransitService index) {
    if (mappedTripsCache.isEmpty()) {
      for (Trip trip : index.getAllTrips()) {
        TripPattern tripPattern = index.getPatternForTrip(trip);

        String currentTripId = getUnpaddedTripId(trip.getId().getId());

        if (mappedTripsCache.containsKey(currentTripId)) {
          mappedTripsCache.get(currentTripId).add(trip);
        } else {
          Set<Trip> initialSet = new HashSet<>();
          initialSet.add(trip);
          mappedTripsCache.put(currentTripId, initialSet);
        }

        if (
          tripPattern != null &&
          tripPattern.matchesModeOrSubMode(TransitMode.RAIL, SubMode.of("railReplacementBus"))
        ) {
          if (trip.getNetexInternalPlanningCode() != null) {
            String internalPlanningCode = trip.getNetexInternalPlanningCode();
            if (mappedVehicleRefCache.containsKey(internalPlanningCode)) {
              mappedVehicleRefCache.get(internalPlanningCode).add(trip);
            } else {
              Set<Trip> initialSet = new HashSet<>();
              initialSet.add(trip);
              mappedVehicleRefCache.put(internalPlanningCode, initialSet);
            }
          }
        }
        String lastStopId = tripPattern.lastStop().getId().getId();

        TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
        if (tripTimes != null) {
          int arrivalTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);

          String key = createStartStopKey(lastStopId, arrivalTime);
          if (start_stop_tripCache.containsKey(key)) {
            start_stop_tripCache.get(key).add(trip);
          } else {
            Set<Trip> initialSet = new HashSet<>();
            initialSet.add(trip);
            start_stop_tripCache.put(key, initialSet);
          }
        }
      }
      for (Route route : index.getAllRoutes()) {
        String currentRouteId = getUnpaddedTripId(route.getId().getId());
        if (mappedRoutesCache.containsKey(currentRouteId)) {
          mappedRoutesCache.get(currentRouteId).add(route);
        } else {
          Set<Route> initialSet = new HashSet<>();
          initialSet.add(route);
          mappedRoutesCache.put(currentRouteId, initialSet);
        }
      }

      LOG.info("Built route-cache [{}].", mappedRoutesCache.size());
      LOG.info("Built vehicleRef-cache [{}].", mappedVehicleRefCache.size());
      LOG.info("Built trips-cache [{}].", mappedTripsCache.size());
      LOG.info("Built start-stop-cache [{}].", start_stop_tripCache.size());
    }

    if (vehicleJourneyTripCache.isEmpty()) {
      index.getAllTrips().forEach(trip -> vehicleJourneyTripCache.put(trip.getId().getId(), trip));
    }
  }

  private static String createStartStopKey(String lastStopId, int lastStopArrivalTime) {
    return lastStopId + ":" + lastStopArrivalTime;
  }

  private static String getUnpaddedTripId(String id) {
    if (id.indexOf("-") > 0) {
      return id.substring(0, id.indexOf("-"));
    } else {
      return id;
    }
  }

  private String resolveDatedVehicleJourneyRef(EstimatedVehicleJourney journey) {
    if (journey.getFramedVehicleJourneyRef() != null) {
      return journey.getFramedVehicleJourneyRef().getDatedVehicleJourneyRef();
    } else if (journey.getDatedVehicleJourneyRef() != null) {
      return journey.getDatedVehicleJourneyRef().getValue();
    }

    return null;
  }

  private Set<Trip> getMatchingTripsOnStopOrSiblings(
    String lastStopPoint,
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

    Set<Trip> trips = start_stop_tripCache.get(
      createStartStopKey(lastStopPoint, secondsSinceMidnight)
    );
    if (trips == null) {
      //Attempt to fetch trips that started yesterday - i.e. add 24 hours to arrival-time
      trips =
        start_stop_tripCache.get(createStartStopKey(lastStopPoint, secondsSinceMidnightYesterday));
    }

    if (trips == null || trips.isEmpty()) {
      //SIRI-data may report other platform, but still on the same Parent-stop
      // TODO OTP2 - We should pass in correct feed id here
      String feedId = transitService.getFeedIds().iterator().next();
      var stop = transitService.getStopForId(new FeedScopedId(feedId, lastStopPoint));
      if (stop != null && stop.isPartOfStation()) {
        // TODO OTP2 resolve stop-station split
        var allQuays = stop.getParentStation().getChildStops();
        for (var quay : allQuays) {
          Set<Trip> tripSet = start_stop_tripCache.get(
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

  private Set<Trip> getCachedTripsByVehicleRef(String vehicleRef) {
    if (vehicleRef == null) {
      return null;
    }
    return mappedVehicleRefCache.getOrDefault(vehicleRef, new HashSet<>());
  }

  private Set<Trip> getCachedTripsBySiriId(String tripId) {
    if (tripId == null) {
      return null;
    }
    return mappedTripsCache.getOrDefault(tripId, new HashSet<>());
  }
}
