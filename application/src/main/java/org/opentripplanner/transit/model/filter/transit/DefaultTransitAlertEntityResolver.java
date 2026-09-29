package org.opentripplanner.transit.model.filter.transit;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

/**
 * Resolves the entities selected by a {@link TransitAlertEntitySelectRequest} by looking them up in
 * the {@link TransitService}.
 * <p>
 * Each entity key is evaluated on its own, so only the entities which actually have alerts are
 * looked up. An entity key is selected if its type is one of the requested types, and it is
 * related to the selected routes and to the selected stops or stations, if they are set. The type
 * of an entity key depends on what it resolves to in the transit data, see {@link #entityType}.
 * <p>
 * If a route is used as a selector, the following logic is used:
 * <ul>
 *   <li>route, stop on route and pattern: the route of the key is selected</li>
 *   <li>trip and stop on trip: the route of the trip is selected</li>
 *   <li>agency: a selected route is operated by the agency</li>
 *   <li>route type: a selected route has the route type (in the feed or for the agency)</li>
 *   <li>stop: a selected route visits the stop or a child stop of the station</li>
 * </ul>
 * If a stop is used as a selector, the following logic is used:
 * <ul>
 *   <li>stop, stop on route and stop on trip: the stop or station of the key is selected,
 *   including the parent stations or child stops if requested</li>
 *   <li>route, pattern and trip: a selected stop is visited</li>
 *   <li>agency: a route of the agency visits a selected stop</li>
 *   <li>route type: never related to stops</li>
 * </ul>
 * When checking if a route, pattern or trip visits a selected stop, a stop is also selected if its
 * parent station is selected.
 * <p>
 * Ids are matched even if the entity doesn't exist in the transit data, but modes can only be
 * matched for existing entities. Entities which don't exist are of type
 * {@link AlertEntityType#UNKNOWN}, so they can only be selected by their ids together with that
 * type.
 */
public class DefaultTransitAlertEntityResolver implements TransitAlertEntityResolver {

  private final TransitService transitService;

  private DefaultTransitAlertEntityResolver(TransitService transitService) {
    this.transitService = Objects.requireNonNull(transitService);
  }

  public static TransitAlertEntityResolver of(TransitService transitService) {
    return new DefaultTransitAlertEntityResolver(transitService);
  }

  @Override
  public Predicate<EntityKey> matcher(TransitAlertEntitySelectRequest request) {
    var entityTypes = request.entityTypes();
    Predicate<EntityKey> routeRelation = request.routes().includeEverything()
      ? key -> true
      : new RouteRelation(request.routes().get())::isRelated;
    Predicate<EntityKey> stopRelation = request.stopsOrStations().includeEverything()
      ? key -> true
      : new StopRelation(request.stopsOrStations().get())::isRelated;

    return key ->
      entityTypes.contains(entityType(key)) && routeRelation.test(key) && stopRelation.test(key);
  }

  /**
   * Returns the type of the entity, based on what it resolves to in the transit data. This follows
   * {@code Alert.entities} in the GTFS GraphQL API: an entity which is returned as {@code Unknown}
   * there is {@link AlertEntityType#UNKNOWN}.
   */
  AlertEntityType entityType(EntityKey key) {
    return switch (key) {
      case EntityKey.Agency k -> typeIfResolved(
        transitService.getAgency(k.agencyId()) != null,
        AlertEntityType.AGENCY
      );
      case EntityKey.DirectionAndRoute k -> typeIfResolved(
        transitService.getRoute(k.routeId()) != null,
        AlertEntityType.PATTERN
      );
      case EntityKey.Route k -> typeIfResolved(
        transitService.getRoute(k.routeId()) != null,
        AlertEntityType.ROUTE
      );
      case EntityKey.RouteType _ -> AlertEntityType.ROUTE_TYPE;
      case EntityKey.RouteTypeAndAgency k -> typeIfResolved(
        transitService.getAgency(k.agencyId()) != null,
        AlertEntityType.ROUTE_TYPE
      );
      case EntityKey.Stop k -> typeIfResolved(
        transitService.getStopLocation(k.stopId()) != null ||
          transitService.getStopLocationsGroup(k.stopId()) != null,
        AlertEntityType.STOP
      );
      case EntityKey.StopAndRoute k -> typeIfResolved(
        transitService.getRegularStop(k.stopId()) != null &&
          transitService.getRoute(k.routeId()) != null,
        AlertEntityType.STOP_ON_ROUTE
      );
      case EntityKey.StopAndTrip k -> typeIfResolved(
        transitService.getRegularStop(k.stopId()) != null &&
          transitService.getTrip(k.tripId()) != null,
        AlertEntityType.STOP_ON_TRIP
      );
      case EntityKey.Trip k -> typeIfResolved(
        transitService.getTrip(k.tripId()) != null,
        AlertEntityType.TRIP
      );
      case EntityKey.Unknown _ -> AlertEntityType.UNKNOWN;
    };
  }

  private static AlertEntityType typeIfResolved(boolean resolved, AlertEntityType type) {
    return resolved ? type : AlertEntityType.UNKNOWN;
  }

  /**
   * Matches an entity against id and mode criteria. The modes are only resolved if needed.
   */
  private static boolean matches(
    FilterValues<FeedScopedId> ids,
    FilterValues<TransitMode> modes,
    FeedScopedId id,
    Supplier<Collection<TransitMode>> entityModes
  ) {
    if (!ids.includeEverything() && !ids.get().contains(id)) {
      return false;
    }
    return modes.includeEverything() || entityModes.get().stream().anyMatch(modes.get()::contains);
  }

  /**
   * Relates entity keys to the selected routes.
   */
  private final class RouteRelation {

    private final Collection<TransitAlertRouteSelectRequest> selectors;
    private final Map<FeedScopedId, Boolean> stopVisitedCache = new HashMap<>();

    @Nullable
    private Set<Route> selectedRoutes;

    private RouteRelation(Collection<TransitAlertRouteSelectRequest> selectors) {
      this.selectors = selectors;
    }

    boolean isRelated(EntityKey key) {
      return switch (key) {
        case EntityKey.Route k -> routeIdSelected(k.routeId());
        case EntityKey.StopAndRoute k -> routeIdSelected(k.routeId());
        case EntityKey.DirectionAndRoute k -> routeIdSelected(k.routeId());
        case EntityKey.Trip k -> tripSelected(k.tripId());
        case EntityKey.StopAndTrip k -> tripSelected(k.tripId());
        case EntityKey.Agency k -> selectedRoutes()
          .stream()
          .anyMatch(r -> r.getAgency().getId().equals(k.agencyId()));
        case EntityKey.RouteType k -> selectedRoutes()
          .stream()
          .anyMatch(
            r ->
              r.getId().getFeedId().equals(k.feedId()) &&
              Objects.equals(r.getGtfsType(), k.routeType())
          );
        case EntityKey.RouteTypeAndAgency k -> selectedRoutes()
          .stream()
          .anyMatch(
            r ->
              r.getAgency().getId().equals(k.agencyId()) &&
              Objects.equals(r.getGtfsType(), k.routeType())
          );
        case EntityKey.Stop k -> stopOrStationVisited(k.stopId());
        case EntityKey.Unknown _ -> false;
      };
    }

    private boolean selected(Route route) {
      return selectors
        .stream()
        .anyMatch(s -> matches(s.ids(), s.modes(), route.getId(), () -> List.of(route.getMode())));
    }

    private boolean routeIdSelected(FeedScopedId routeId) {
      var route = transitService.getRoute(routeId);
      if (route != null) {
        return selected(route);
      }
      // the route doesn't exist in the transit data, so only the id can be matched
      return selectors.stream().anyMatch(s -> matches(s.ids(), s.modes(), routeId, List::of));
    }

    private boolean tripSelected(FeedScopedId tripId) {
      var trip = transitService.getTrip(tripId);
      return trip != null && selected(trip.getRoute());
    }

    private boolean stopOrStationVisited(FeedScopedId id) {
      var stop = transitService.getStopLocation(id);
      if (stop != null) {
        return stopVisited(stop);
      }
      var station = transitService.getStation(id);
      return station != null && station.getChildStops().stream().anyMatch(this::stopVisited);
    }

    private boolean stopVisited(StopLocation stop) {
      return stopVisitedCache.computeIfAbsent(stop.getId(), ignore ->
        transitService.findRoutes(stop).stream().anyMatch(this::selected)
      );
    }

    private Set<Route> selectedRoutes() {
      if (selectedRoutes == null) {
        var routes = new HashSet<Route>();
        for (var selector : selectors) {
          if (selector.ids().includeEverything()) {
            transitService.listRoutes().stream().filter(this::selected).forEach(routes::add);
          } else {
            selector
              .ids()
              .get()
              .stream()
              .map(transitService::getRoute)
              .filter(Objects::nonNull)
              .filter(this::selected)
              .forEach(routes::add);
          }
        }
        selectedRoutes = routes;
      }
      return selectedRoutes;
    }
  }

  /**
   * Relates entity keys to the selected stops and stations.
   */
  private final class StopRelation {

    private final Collection<TransitAlertStopOrStationSelectRequest> selectors;
    private final Map<FeedScopedId, Boolean> visitedStopCache = new HashMap<>();
    private final Map<FeedScopedId, Boolean> routeCache = new HashMap<>();

    private StopRelation(Collection<TransitAlertStopOrStationSelectRequest> selectors) {
      this.selectors = selectors;
    }

    boolean isRelated(EntityKey key) {
      return switch (key) {
        case EntityKey.Stop k -> locationSelected(k.stopId());
        case EntityKey.StopAndRoute k -> locationSelected(k.stopId());
        case EntityKey.StopAndTrip k -> locationSelected(k.stopId());
        case EntityKey.Route k -> {
          var route = transitService.getRoute(k.routeId());
          yield route != null && routeVisitsSelectedStop(route);
        }
        case EntityKey.DirectionAndRoute k -> {
          var route = transitService.getRoute(k.routeId());
          yield route != null &&
            transitService
              .findPatterns(route)
              .stream()
              .filter(p -> p.getDirection() == k.direction())
              .anyMatch(this::patternVisitsSelectedStop);
        }
        case EntityKey.Trip k -> {
          var trip = transitService.getTrip(k.tripId());
          var pattern = trip == null ? null : transitService.findPattern(trip);
          yield pattern != null && patternVisitsSelectedStop(pattern);
        }
        case EntityKey.Agency k -> transitService
          .listRoutes()
          .stream()
          .filter(r -> r.getAgency().getId().equals(k.agencyId()))
          .anyMatch(this::routeVisitsSelectedStop);
        case EntityKey.RouteType _ -> false;
        case EntityKey.RouteTypeAndAgency _ -> false;
        case EntityKey.Unknown _ -> false;
      };
    }

    /**
     * Returns true if the stop or station with the given id is selected, including the expansions
     * to parent stations and child stops.
     */
    private boolean locationSelected(FeedScopedId id) {
      var stop = transitService.getStopLocation(id);
      if (stop != null) {
        var parent = stop.getParentStation();
        return selectors
          .stream()
          .anyMatch(
            s ->
              selected(s, stop) ||
              (s.includeChildStopAlerts() && parent != null && selected(s, parent))
          );
      }
      var station = transitService.getStation(id);
      if (station != null) {
        return selectors.stream().anyMatch(
          s ->
            selected(s, station) ||
            (s.includeParentStationAlerts() &&
              station
                .getChildStops()
                .stream()
                .anyMatch(child -> selected(s, child)))
        );
      }
      // the location doesn't exist in the transit data, so only the id can be matched
      return selectors.stream().anyMatch(s -> matches(s.ids(), s.modes(), id, List::of));
    }

    private boolean routeVisitsSelectedStop(Route route) {
      return routeCache.computeIfAbsent(route.getId(), ignore ->
        transitService.findPatterns(route).stream().anyMatch(this::patternVisitsSelectedStop)
      );
    }

    private boolean patternVisitsSelectedStop(TripPattern pattern) {
      return pattern.getStops().stream().anyMatch(this::visitedStopSelected);
    }

    /**
     * A stop visited by a route, pattern or trip is selected if the stop itself or its parent
     * station is selected.
     */
    private boolean visitedStopSelected(StopLocation stop) {
      return visitedStopCache.computeIfAbsent(stop.getId(), ignore -> {
        var parent = stop.getParentStation();
        return selectors
          .stream()
          .anyMatch(s -> selected(s, stop) || (parent != null && selected(s, parent)));
      });
    }

    private boolean selected(TransitAlertStopOrStationSelectRequest selector, StopLocation stop) {
      return matches(selector.ids(), selector.modes(), stop.getId(), () ->
        transitService.findTransitModes(stop)
      );
    }

    private boolean selected(TransitAlertStopOrStationSelectRequest selector, Station station) {
      return matches(selector.ids(), selector.modes(), station.getId(), () ->
        transitService.findTransitModes(station)
      );
    }
  }
}
