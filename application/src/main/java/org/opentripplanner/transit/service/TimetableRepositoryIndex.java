package org.opentripplanner.transit.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexed access to Transit entities.
 * For performance reasons these indexes are not part of the serialized state of the graph.
 * They are rebuilt at runtime after graph deserialization.
 */
class TimetableRepositoryIndex {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableRepositoryIndex.class);

  // TODO: consistently key on model object or id string
  private final Map<FeedScopedId, Agency> agencyForId = new HashMap<>();
  private final Map<FeedScopedId, Operator> operatorForId = new HashMap<>();

  private final Map<FeedScopedId, Trip> tripForId = new HashMap<>();
  private final Map<FeedScopedId, Route> routeForId = new HashMap<>();

  private final Map<Trip, TripPattern> patternForTrip = new HashMap<>();
  private final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
  private final Multimap<StopLocation, TripPattern> patternsForStop = ArrayListMultimap.create();

  private Map<StopLocation, LocalDate> endOfServiceDateForStop = new HashMap<>();
  private final Map<LocalDate, TIntSet> serviceCodesRunningForDate = new HashMap<>();
  private final Map<TripIdAndServiceDate, TripOnServiceDate> tripOnServiceDateForTripAndDay =
    new HashMap<>();

  private final Multimap<GroupOfRoutes, Route> routesForGroupOfRoutes = ArrayListMultimap.create();

  private final Map<FeedScopedId, GroupOfRoutes> groupOfRoutesForId = new HashMap<>();
  private FlexIndex flexIndex = null;

  TimetableRepositoryIndex(TimetableRepository timetableRepository) {
    LOG.info("Timetable repository index init...");

    for (Agency agency : timetableRepository.getAgencies()) {
      this.agencyForId.put(agency.getId(), agency);
    }

    for (Operator operator : timetableRepository.getOperators()) {
      this.operatorForId.put(operator.getId(), operator);
    }

    for (TripPattern pattern : timetableRepository.getAllTripPatterns()) {
      patternsForRoute.put(pattern.getRoute(), pattern);
      pattern
        .scheduledTripsAsStream()
        .forEach(trip -> {
          patternForTrip.put(trip, pattern);
          tripForId.put(trip.getId(), trip);
        });
      for (StopLocation stop : pattern.getStops()) {
        patternsForStop.put(stop, pattern);
      }
    }
    for (Route route : patternsForRoute.asMap().keySet()) {
      routeForId.put(route.getId(), route);
      for (GroupOfRoutes groupOfRoutes : route.getGroupsOfRoutes()) {
        routesForGroupOfRoutes.put(groupOfRoutes, route);
      }
    }
    for (GroupOfRoutes groupOfRoutes : routesForGroupOfRoutes.keySet()) {
      groupOfRoutesForId.put(groupOfRoutes.getId(), groupOfRoutes);
    }

    for (TripOnServiceDate tripOnServiceDate : timetableRepository.getAllTripsOnServiceDates()) {
      tripOnServiceDateForTripAndDay.put(
        new TripIdAndServiceDate(
          tripOnServiceDate.getTrip().getId(),
          tripOnServiceDate.getServiceDate()
        ),
        tripOnServiceDate
      );
    }

    initializeServiceData(timetableRepository);

    if (OTPFeature.FlexRouting.isOn()) {
      flexIndex = new FlexIndex(timetableRepository);
      for (Route route : flexIndex.getAllFlexRoutes()) {
        routeForId.put(route.getId(), route);
      }
      for (FlexTrip flexTrip : flexIndex.getAllFlexTrips()) {
        tripForId.put(flexTrip.getId(), flexTrip.getTrip());
      }
    }

    LOG.info("Timetable repository index init complete.");
  }

  Agency getAgencyForId(FeedScopedId id) {
    return agencyForId.get(id);
  }

  Route getRouteForId(FeedScopedId id) {
    return routeForId.get(id);
  }

  /** Dynamically generate the set of Routes passing though a Stop on demand. */
  Set<Route> getRoutesForStop(StopLocation stop) {
    Set<Route> routes = new HashSet<>();
    for (TripPattern p : patternsForStop.get(stop)) {
      routes.add(p.getRoute());
    }
    return routes;
  }

  Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    return Collections.unmodifiableCollection(patternsForStop.get(stop));
  }

  Collection<Trip> getTripsForStop(StopLocation stop) {
    return patternsForStop
      .get(stop)
      .stream()
      .flatMap(TripPattern::scheduledTripsAsStream)
      .collect(Collectors.toList());
  }

  /**
   * Checks if the last scheduled service date for the stop is on or after the given date.
   * This does not include real-time updates, so it only checks the scheduled service dates.
   *
   * @param date the date to check against
   * @param stop the stop to check
   * @return true if the stop has scheduled services after the given date, false otherwise
   */
  boolean hasScheduledServicesAfter(LocalDate date, StopLocation stop) {
    LocalDate endOfServiceDate = endOfServiceDateForStop.get(stop);
    return (
      endOfServiceDate != null && (endOfServiceDate.isAfter(date) || endOfServiceDate.isEqual(date))
    );
  }

  Operator getOperatorForId(FeedScopedId operatorId) {
    return operatorForId.get(operatorId);
  }

  Collection<Trip> getAllTrips() {
    return Collections.unmodifiableCollection(tripForId.values());
  }

  Trip getTripForId(FeedScopedId tripId) {
    return tripForId.get(tripId);
  }

  /**
   * Checks if the specified trip is contained within the index.
   *
   * @param tripId the {@link FeedScopedId} of the trip to check
   * @return true if the trip exists in the index map, false otherwise
   */
  boolean containsTrip(FeedScopedId tripId) {
    return tripForId.containsKey(tripId);
  }

  TripOnServiceDate getTripOnServiceDateForTripAndDay(TripIdAndServiceDate tripIdAndServiceDate) {
    return tripOnServiceDateForTripAndDay.get(tripIdAndServiceDate);
  }

  Collection<Route> getAllRoutes() {
    return Collections.unmodifiableCollection(routeForId.values());
  }

  TripPattern getPatternForTrip(Trip trip) {
    return patternForTrip.get(trip);
  }

  Collection<TripPattern> getPatternsForRoute(Route route) {
    return Collections.unmodifiableCollection(patternsForRoute.get(route));
  }

  Map<LocalDate, TIntSet> getServiceCodesRunningForDate() {
    return serviceCodesRunningForDate;
  }

  FlexIndex getFlexIndex() {
    return flexIndex;
  }

  private void initializeServiceData(TimetableRepository timetableRepository) {
    CalendarService calendarService = timetableRepository.getCalendarService();

    if (calendarService == null) {
      return;
    }

    // CalendarService has one main implementation (CalendarServiceImpl) which contains a
    // CalendarServiceData which can easily supply all of the dates. But it's impossible to
    // actually see those dates without modifying the interfaces and inheritance. So we have
    // to work around this abstraction and reconstruct the CalendarData.
    // Note the "multiCalendarServiceImpl" which has docs saying it expects one single
    // CalendarData. It seems to merge the calendar services from multiple GTFS feeds, but
    // its only documentation says it's a hack.
    // TODO OTP2 - This cleanup is added to the 'Final cleanup OTP2' issue #2757

    // Reconstruct set of all dates where service is defined, keeping track of which services
    // run on which days.
    Multimap<LocalDate, FeedScopedId> serviceIdsForServiceDate = HashMultimap.create();
    Map<FeedScopedId, LocalDate> endOfServiceDateForService = new HashMap<>();

    for (FeedScopedId serviceId : calendarService.getServiceIds()) {
      Set<LocalDate> serviceDatesForService = calendarService.getServiceDatesForServiceId(
        serviceId
      );
      for (LocalDate serviceDate : serviceDatesForService) {
        serviceIdsForServiceDate.put(serviceDate, serviceId);

        // Save the last service date for each service.
        if (
          endOfServiceDateForService.get(serviceId) == null ||
          (serviceDate != null && serviceDate.isAfter(endOfServiceDateForService.get(serviceId)))
        ) {
          endOfServiceDateForService.put(serviceId, serviceDate);
        }
      }
    }
    for (LocalDate serviceDate : serviceIdsForServiceDate.keySet()) {
      TIntSet serviceCodesRunning = new TIntHashSet();
      for (FeedScopedId serviceId : serviceIdsForServiceDate.get(serviceDate)) {
        serviceCodesRunning.add(timetableRepository.getServiceCodes().get(serviceId));
      }
      serviceCodesRunningForDate.put(serviceDate, serviceCodesRunning);
    }

    initializeTheEndOfServiceDateForStop(endOfServiceDateForService);
  }

  private void initializeTheEndOfServiceDateForStop(
    Map<FeedScopedId, LocalDate> endOfServiceDateForService
  ) {
    Map<StopLocation, LocalDate> endOfServiceDates = new HashMap<>();
    for (StopLocation stop : patternsForStop.keySet()) {
      for (TripPattern pattern : patternsForStop.get(stop)) {
        pattern
          .scheduledTripsAsStream()
          .forEach(trip -> {
            LocalDate tripEndDate = endOfServiceDateForService.get(trip.getServiceId());
            LocalDate endOfServiceDate = endOfServiceDates.get(stop);
            if (
              tripEndDate != null &&
              (endOfServiceDate == null || tripEndDate.isAfter(endOfServiceDate))
            ) {
              endOfServiceDates.put(stop, tripEndDate);
            }
          });
      }
    }
    endOfServiceDateForStop = Map.copyOf(endOfServiceDates);
  }

  Collection<GroupOfRoutes> getAllGroupOfRoutes() {
    return Collections.unmodifiableCollection(groupOfRoutesForId.values());
  }

  Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes) {
    return Collections.unmodifiableCollection(routesForGroupOfRoutes.get(groupOfRoutes));
  }

  GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id) {
    return groupOfRoutesForId.get(id);
  }
}
