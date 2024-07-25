package org.opentripplanner.transit.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.time.LocalDate;
import java.util.Collection;
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
public class TransitModelIndex {

  private static final Logger LOG = LoggerFactory.getLogger(TransitModelIndex.class);

  // TODO: consistently key on model object or id string
  private final Map<FeedScopedId, Agency> agencyForId = new HashMap<>();
  private final Map<FeedScopedId, Operator> operatorForId = new HashMap<>();

  private final Map<FeedScopedId, Trip> tripForId = new HashMap<>();
  private final Map<FeedScopedId, Route> routeForId = new HashMap<>();

  private final Map<Trip, TripPattern> patternForTrip = new HashMap<>();
  private final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
  private final Multimap<StopLocation, TripPattern> patternsForStopId = ArrayListMultimap.create();

  private final Map<LocalDate, TIntSet> serviceCodesRunningForDate = new HashMap<>();
  private final Map<FeedScopedId, TripOnServiceDate> tripOnServiceDateById = new HashMap<>();
  private final Map<TripIdAndServiceDate, TripOnServiceDate> tripOnServiceDateForTripAndDay = new HashMap<>();

  private final Multimap<GroupOfRoutes, Route> routesForGroupOfRoutes = ArrayListMultimap.create();

  private final Map<FeedScopedId, GroupOfRoutes> groupOfRoutesForId = new HashMap<>();
  private FlexIndex flexIndex = null;

  TransitModelIndex(TransitModel transitModel) {
    LOG.info("Transit model index init...");

    for (Agency agency : transitModel.getAgencies()) {
      this.agencyForId.put(agency.getId(), agency);
    }

    for (Operator operator : transitModel.getOperators()) {
      this.operatorForId.put(operator.getId(), operator);
    }

    for (TripPattern pattern : transitModel.getAllTripPatterns()) {
      patternsForRoute.put(pattern.getRoute(), pattern);
      pattern
        .scheduledTripsAsStream()
        .forEach(trip -> {
          patternForTrip.put(trip, pattern);
          tripForId.put(trip.getId(), trip);
        });
      for (StopLocation stop : pattern.getStops()) {
        patternsForStopId.put(stop, pattern);
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

    for (TripOnServiceDate tripOnServiceDate : transitModel.getAllTripOnServiceDates()) {
      tripOnServiceDateById.put(tripOnServiceDate.getId(), tripOnServiceDate);
      tripOnServiceDateForTripAndDay.put(
        new TripIdAndServiceDate(
          tripOnServiceDate.getTrip().getId(),
          tripOnServiceDate.getServiceDate()
        ),
        tripOnServiceDate
      );
    }

    initalizeServiceCodesForDate(transitModel);

    if (OTPFeature.FlexRouting.isOn()) {
      flexIndex = new FlexIndex(transitModel);
      for (Route route : flexIndex.getAllFlexRoutes()) {
        routeForId.put(route.getId(), route);
      }
      for (FlexTrip flexTrip : flexIndex.getAllFlexTrips()) {
        tripForId.put(flexTrip.getId(), flexTrip.getTrip());
      }
    }

    LOG.info("Transit Model index init complete.");
  }

  public Agency getAgencyForId(FeedScopedId id) {
    return agencyForId.get(id);
  }

  public Route getRouteForId(FeedScopedId id) {
    return routeForId.get(id);
  }

  public void addRoutes(Route route) {
    routeForId.put(route.getId(), route);
  }

  /** Dynamically generate the set of Routes passing though a Stop on demand. */
  public Set<Route> getRoutesForStop(StopLocation stop) {
    Set<Route> routes = new HashSet<>();
    for (TripPattern p : getPatternsForStop(stop)) {
      routes.add(p.getRoute());
    }
    return routes;
  }

  public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    return patternsForStopId.get(stop);
  }

  public Collection<Trip> getTripsForStop(StopLocation stop) {
    return getPatternsForStop(stop)
      .stream()
      .flatMap(TripPattern::scheduledTripsAsStream)
      .collect(Collectors.toList());
  }

  /**
   * Get a list of all operators spanning across all feeds.
   */
  public Collection<Operator> getAllOperators() {
    return getOperatorForId().values();
  }

  public Map<FeedScopedId, Operator> getOperatorForId() {
    return operatorForId;
  }

  public Map<FeedScopedId, Trip> getTripForId() {
    return tripForId;
  }

  public Map<FeedScopedId, TripOnServiceDate> getTripOnServiceDateById() {
    return tripOnServiceDateById;
  }

  public Map<TripIdAndServiceDate, TripOnServiceDate> getTripOnServiceDateForTripAndDay() {
    return tripOnServiceDateForTripAndDay;
  }

  public Collection<Route> getAllRoutes() {
    return routeForId.values();
  }

  public Map<Trip, TripPattern> getPatternForTrip() {
    return patternForTrip;
  }

  public Multimap<Route, TripPattern> getPatternsForRoute() {
    return patternsForRoute;
  }

  public Map<LocalDate, TIntSet> getServiceCodesRunningForDate() {
    return serviceCodesRunningForDate;
  }

  public FlexIndex getFlexIndex() {
    return flexIndex;
  }

  private void initalizeServiceCodesForDate(TransitModel transitModel) {
    CalendarService calendarService = transitModel.getCalendarService();

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

    for (FeedScopedId serviceId : calendarService.getServiceIds()) {
      Set<LocalDate> serviceDatesForService = calendarService.getServiceDatesForServiceId(
        serviceId
      );
      for (LocalDate serviceDate : serviceDatesForService) {
        serviceIdsForServiceDate.put(serviceDate, serviceId);
      }
    }
    for (LocalDate serviceDate : serviceIdsForServiceDate.keySet()) {
      TIntSet serviceCodesRunning = new TIntHashSet();
      for (FeedScopedId serviceId : serviceIdsForServiceDate.get(serviceDate)) {
        serviceCodesRunning.add(transitModel.getServiceCodes().get(serviceId));
      }
      serviceCodesRunningForDate.put(serviceDate, serviceCodesRunning);
    }
  }

  public Multimap<GroupOfRoutes, Route> getRoutesForGroupOfRoutes() {
    return routesForGroupOfRoutes;
  }

  public Map<FeedScopedId, GroupOfRoutes> getGroupOfRoutesForId() {
    return groupOfRoutesForId;
  }
}
