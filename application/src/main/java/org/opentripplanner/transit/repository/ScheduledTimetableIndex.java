package org.opentripplanner.transit.repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The derived lookups (by id, by trip, by route, by stop, ...) over the scheduled data held by
 * {@link ScheduledTimetableData}. Built once, when the immutable scheduled data is created.
 */
final class ScheduledTimetableIndex {

  private static final Logger LOG = LoggerFactory.getLogger(ScheduledTimetableIndex.class);

  private final Map<FeedScopedId, Route> routeForId = new HashMap<>();
  private final Map<FeedScopedId, Trip> tripForId = new HashMap<>();
  private final Map<Trip, TripPattern> patternForTrip = new HashMap<>();
  private final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
  private final Multimap<StopLocation, TripPattern> patternsForStop = ArrayListMultimap.create();
  private final Map<TripIdAndServiceDate, TripOnServiceDate> tripOnServiceDateForTripAndDay =
    new HashMap<>();
  private final Multimap<GroupOfRoutes, Route> routesForGroupOfRoutes = ArrayListMultimap.create();
  private final Map<FeedScopedId, GroupOfRoutes> groupOfRoutesForId = new HashMap<>();
  private final Map<StopLocation, LocalDate> endOfServiceDateForStop;

  @Nullable
  private final FlexIndex flexIndex;

  ScheduledTimetableIndex(ScheduledTimetableData entities, TripCalendars tripCalendars) {
    LOG.info("Index scheduled timetable data...");

    for (TripPattern pattern : entities.getAllTripPatterns()) {
      patternsForRoute.put(pattern.getRoute(), pattern);
      pattern.scheduledTripsAsStream().forEach(trip -> {
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
    for (TripOnServiceDate tripOnServiceDate : entities.getAllTripsOnServiceDate()) {
      tripOnServiceDateForTripAndDay.put(
        new TripIdAndServiceDate(
          tripOnServiceDate.getTrip().getId(),
          tripOnServiceDate.getServiceDate()
        ),
        tripOnServiceDate
      );
    }

    this.endOfServiceDateForStop = endOfServiceDateForStop(tripCalendars);

    // Flex trips/routes are only visible when flex routing is enabled, even if the graph was
    // built with it.
    if (OTPFeature.FlexRouting.isOn()) {
      flexIndex = new FlexIndex(entities.getAllFlexTrips(), tripCalendars);
      for (Route route : flexIndex.getAllFlexRoutes()) {
        routeForId.put(route.getId(), route);
      }
      for (FlexTrip<?, ?> flexTrip : flexIndex.getAllFlexTrips()) {
        tripForId.put(flexTrip.getId(), flexTrip.getTrip());
      }
    } else {
      flexIndex = null;
    }

    LOG.info("Index scheduled timetable data complete.");
  }

  @Nullable
  Route getRouteForId(FeedScopedId id) {
    return routeForId.get(id);
  }

  Collection<Route> getAllRoutes() {
    return Collections.unmodifiableCollection(routeForId.values());
  }

  @Nullable
  Trip getTripForId(FeedScopedId id) {
    return tripForId.get(id);
  }

  Collection<Trip> getAllTrips() {
    return Collections.unmodifiableCollection(tripForId.values());
  }

  boolean containsTrip(FeedScopedId id) {
    return tripForId.containsKey(id);
  }

  @Nullable
  TripPattern getPatternForTrip(Trip trip) {
    return patternForTrip.get(trip);
  }

  Collection<TripPattern> getPatternsForRoute(Route route) {
    return Collections.unmodifiableCollection(patternsForRoute.get(route));
  }

  Collection<TripPattern> getPatternsForStop(StopLocation stop) {
    return Collections.unmodifiableCollection(patternsForStop.get(stop));
  }

  Set<Route> getRoutesForStop(StopLocation stop) {
    Set<Route> routes = new HashSet<>();
    for (TripPattern p : patternsForStop.get(stop)) {
      routes.add(p.getRoute());
    }
    return routes;
  }

  @Nullable
  TripOnServiceDate getTripOnServiceDateForTripAndDay(TripIdAndServiceDate tripIdAndServiceDate) {
    return tripOnServiceDateForTripAndDay.get(tripIdAndServiceDate);
  }

  Collection<GroupOfRoutes> getAllGroupOfRoutes() {
    return Collections.unmodifiableCollection(groupOfRoutesForId.values());
  }

  Collection<Route> getRoutesForGroupOfRoutes(GroupOfRoutes groupOfRoutes) {
    return Collections.unmodifiableCollection(routesForGroupOfRoutes.get(groupOfRoutes));
  }

  @Nullable
  GroupOfRoutes getGroupOfRoutesForId(FeedScopedId id) {
    return groupOfRoutesForId.get(id);
  }

  boolean hasScheduledServicesAfter(LocalDate date, StopLocation stop) {
    LocalDate endOfServiceDate = endOfServiceDateForStop.get(stop);
    return endOfServiceDate != null && !endOfServiceDate.isBefore(date);
  }

  @Nullable
  FlexIndex getFlexIndex() {
    return flexIndex;
  }

  private Map<StopLocation, LocalDate> endOfServiceDateForStop(TripCalendars tripCalendars) {
    Map<FeedScopedId, LocalDate> endOfServiceDateForService = new HashMap<>();
    for (FeedScopedId serviceId : tripCalendars.listServiceIds()) {
      for (LocalDate serviceDate : tripCalendars.listServiceDates(serviceId)) {
        endOfServiceDateForService.merge(serviceId, serviceDate, (a, b) -> a.isAfter(b) ? a : b);
      }
    }

    Map<StopLocation, LocalDate> endOfServiceDates = new HashMap<>();
    for (StopLocation stop : patternsForStop.keySet()) {
      for (TripPattern pattern : patternsForStop.get(stop)) {
        pattern.scheduledTripsAsStream().forEach(trip -> {
          LocalDate tripEndDate = endOfServiceDateForService.get(trip.getServiceId());
          if (tripEndDate != null) {
            endOfServiceDates.merge(stop, tripEndDate, (a, b) -> a.isAfter(b) ? a : b);
          }
        });
      }
    }
    return Map.copyOf(endOfServiceDates);
  }
}
