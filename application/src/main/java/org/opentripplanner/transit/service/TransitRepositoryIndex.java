package org.opentripplanner.transit.service;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final Map<FeedScopedId, Agency> agencyForId;
  private final Map<FeedScopedId, Operator> operatorForId;

  private final Map<FeedScopedId, Trip> tripForId;
  private final Map<FeedScopedId, Route> routeForId;

  private final Map<Trip, TripPattern> patternForTrip;
  private final Multimap<Route, TripPattern> patternsForRoute;
  private final Multimap<StopLocation, TripPattern> patternsForStop;

  private final Map<StopLocation, LocalDate> endOfServiceDateForStop;
  private final Map<TripIdAndServiceDate, TripOnServiceDate> tripOnServiceDateForTripAndDay;

  private final Multimap<GroupOfRoutes, Route> routesForGroupOfRoutes;

  private final Map<FeedScopedId, GroupOfRoutes> groupOfRoutesForId;
  private FlexIndex flexIndex = null;

  TransitRepositoryIndex(TransitRepository transitRepository) {
    LOG.info("Timetable repository index init...");

    this.agencyForId = transitRepository
      .getAgencies()
      .stream()
      .collect(Collectors.toUnmodifiableMap(Agency::getId, Function.identity()));

    this.operatorForId = transitRepository
      .getOperators()
      .stream()
      .collect(Collectors.toUnmodifiableMap(Operator::getId, Function.identity()));

    // tripForId and routeForId are amended further down by the flex-routing block below, so they
    // stay mutable until the very end of the constructor, when they are made immutable.
    Map<FeedScopedId, Trip> tripForIdBuilder = new HashMap<>();
    Map<FeedScopedId, Route> routeForIdBuilder = new HashMap<>();
    Map<Trip, TripPattern> patternForTripBuilder = new HashMap<>();
    ImmutableListMultimap.Builder<Route, TripPattern> patternsForRouteBuilder =
      ImmutableListMultimap.builder();
    ImmutableListMultimap.Builder<StopLocation, TripPattern> patternsForStopBuilder =
      ImmutableListMultimap.builder();

    for (TripPattern pattern : transitRepository.getAllTripPatterns()) {
      patternsForRouteBuilder.put(pattern.getRoute(), pattern);
      pattern
        .scheduledTripsAsStream()
        .forEach(trip -> {
          patternForTripBuilder.put(trip, pattern);
          tripForIdBuilder.put(trip.getId(), trip);
        });
      for (StopLocation stop : pattern.getStops()) {
        patternsForStopBuilder.put(stop, pattern);
      }
    }
    this.patternForTrip = Map.copyOf(patternForTripBuilder);
    this.patternsForRoute = patternsForRouteBuilder.build();
    this.patternsForStop = patternsForStopBuilder.build();

    ImmutableListMultimap.Builder<GroupOfRoutes, Route> routesForGroupOfRoutesBuilder =
      ImmutableListMultimap.builder();
    for (Route route : patternsForRoute.asMap().keySet()) {
      routeForIdBuilder.put(route.getId(), route);
      for (GroupOfRoutes groupOfRoutes : route.getGroupsOfRoutes()) {
        routesForGroupOfRoutesBuilder.put(groupOfRoutes, route);
      }
    }
    this.routesForGroupOfRoutes = routesForGroupOfRoutesBuilder.build();
    this.groupOfRoutesForId = routesForGroupOfRoutes
      .keySet()
      .stream()
      .collect(Collectors.toUnmodifiableMap(GroupOfRoutes::getId, Function.identity()));

    this.tripOnServiceDateForTripAndDay = transitRepository
      .getAllTripsOnServiceDates()
      .stream()
      .collect(
        Collectors.toUnmodifiableMap(
          tripOnServiceDate ->
            new TripIdAndServiceDate(
              tripOnServiceDate.getTrip().getId(),
              tripOnServiceDate.getServiceDate()
            ),
          Function.identity()
        )
      );

    this.endOfServiceDateForStop = initializeServiceData(transitRepository.getTripCalendar());

    if (OTPFeature.FlexRouting.isOn()) {
      flexIndex = new FlexIndex(transitRepository);
      for (Route route : flexIndex.getAllFlexRoutes()) {
        routeForIdBuilder.put(route.getId(), route);
      }
      for (FlexTrip flexTrip : flexIndex.getAllFlexTrips()) {
        tripForIdBuilder.put(flexTrip.getId(), flexTrip.getTrip());
      }
    }
    this.tripForId = Map.copyOf(tripForIdBuilder);
    this.routeForId = Map.copyOf(routeForIdBuilder);

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
    return patternsForStop.get(stop);
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
    return tripForId.values();
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
    return routeForId.values();
  }

  TripPattern getPatternForTrip(Trip trip) {
    return patternForTrip.get(trip);
  }

  Collection<TripPattern> getPatternsForRoute(Route route) {
    return patternsForRoute.get(route);
  }

  FlexIndex getFlexIndex() {
    return flexIndex;
  }

  private Map<StopLocation, LocalDate> initializeServiceData(TripCalendars tripCalendar) {
    if (tripCalendar == null) {
      return Map.of();
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
    return initializeTheEndOfServiceDateForStop(endOfServiceDateForService);
  }

  private Map<StopLocation, LocalDate> initializeTheEndOfServiceDateForStop(
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
    return Map.copyOf(endOfServiceDates);
  }

  Collection<GroupOfRoutes> getAllGroupOfRoutes() {
    return groupOfRoutesForId.values();
  }

  Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes) {
    return routesForGroupOfRoutes.get(groupOfRoutes);
  }

  GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id) {
    return groupOfRoutesForId.get(id);
  }
}
