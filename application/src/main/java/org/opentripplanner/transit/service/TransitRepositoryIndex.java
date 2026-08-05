package org.opentripplanner.transit.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.model.calendar.TripCalendars;
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
class TransitRepositoryIndex {

  private static final Logger LOG = LoggerFactory.getLogger(TransitRepositoryIndex.class);

  // TODO: consistently key on model object or id string
  private final Map<FeedScopedId, Agency> agencyForId = new HashMap<>();
  private final Map<FeedScopedId, Operator> operatorForId = new HashMap<>();

  private final Map<FeedScopedId, Trip> tripForId = new HashMap<>();
  private final Map<FeedScopedId, Route> routeForId = new HashMap<>();

  private final Map<Trip, TripPattern> patternForTrip = new HashMap<>();
  private final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
  private final Multimap<StopLocation, TripPattern> patternsForStop = ArrayListMultimap.create();

  private Map<StopLocation, LocalDate> endOfServiceDateForStop = new HashMap<>();
  private final Map<TripIdAndServiceDate, TripOnServiceDate> tripOnServiceDateForTripAndDay =
    new HashMap<>();

  private final Multimap<GroupOfRoutes, Route> routesForGroupOfRoutes = ArrayListMultimap.create();

  private final Map<FeedScopedId, GroupOfRoutes> groupOfRoutesForId = new HashMap<>();
  private FlexIndex flexIndex = null;

  TransitRepositoryIndex(TransitRepository transitRepository) {
    LOG.info("Timetable repository index init...");

    for (Agency agency : transitRepository.getAgencies()) {
      this.agencyForId.put(agency.getId(), agency);
    }

    for (Operator operator : transitRepository.getOperators()) {
      this.operatorForId.put(operator.getId(), operator);
    }

    for (TripPattern pattern : transitRepository.getAllTripPatterns()) {
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

    for (TripOnServiceDate tripOnServiceDate : transitRepository.getAllTripsOnServiceDates()) {
      tripOnServiceDateForTripAndDay.put(
        new TripIdAndServiceDate(
          tripOnServiceDate.getTrip().getId(),
          tripOnServiceDate.getServiceDate()
        ),
        tripOnServiceDate
      );
    }

    initializeServiceData(transitRepository.getTripCalendar());

    if (OTPFeature.FlexRouting.isOn()) {
      flexIndex = new FlexIndex(transitRepository);
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

  FlexIndex getFlexIndex() {
    return flexIndex;
  }

  private void initializeServiceData(TripCalendars tripCalendar) {
    if (tripCalendar == null) {
      return;
    }
    // Reconstruct set of all dates where service is defined, keeping track of which services
    // run on which days.
    Map<FeedScopedId, LocalDate> endOfServiceDateForService = new HashMap<>();

    for (FeedScopedId serviceId : tripCalendar.listServiceIds()) {
      Set<LocalDate> serviceDatesForService = tripCalendar.listServiceDates(serviceId);
      for (LocalDate serviceDate : serviceDatesForService) {
        var endDate = endOfServiceDateForService.get(serviceId);
        if (endDate == null || serviceDate.isAfter(endDate)) {
          endOfServiceDateForService.put(serviceId, serviceDate);
        }
      }
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
