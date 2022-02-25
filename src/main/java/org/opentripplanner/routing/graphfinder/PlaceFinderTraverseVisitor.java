package org.opentripplanner.routing.graphfinder;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;

/**
 * A TraverseVisitor used in finding various types of places while walking the street graph.
 *
 * TODO Add car and bike parks
 */
public class PlaceFinderTraverseVisitor implements TraverseVisitor {

  public final List<PlaceAtDistance> placesFound = new ArrayList<>();
  private final RoutingService routingService;
  private final Set<TransitMode> filterByModes;
  private final Set<FeedScopedId> filterByStops;
  private final Set<FeedScopedId> filterByRoutes;
  private final Set<String> filterByBikeRentalStation;
  private final Set<String> seenPatternAtStops = new HashSet<>();
  private final Set<FeedScopedId> seenStops = new HashSet<>();
  private final Set<FeedScopedId> seenBicycleRentalStations = new HashSet<>();
  private final boolean includeStops;
  private final boolean includePatternAtStops;
  private final boolean includeBikeShares;
  private final int maxResults;
  private final double radiusMeters;

  /**
   *
   * @param routingService A RoutingService used in finding information about the various places.
   * @param filterByModes A list of TransitModes for which to find Stops and PatternAtStops. Use null to disable the filtering.
   * @param filterByPlaceTypes A list of PlaceTypes to search for. Use null to disable the filtering, and search for all types.
   * @param filterByStops A list of Stop ids for which to find Stops and PatternAtStops. Use null to disable the filtering.
   * @param filterByRoutes A list of Route ids used for filtering Stops. Only the stops which are served by the route are returned. Use null to disable the filtering.
   * @param filterByBikeRentalStations A list of VehicleRentalStation ids to use in filtering.  Use null to disable the filtering.
   * @param maxResults Maximum number of results to return.
   */
  public PlaceFinderTraverseVisitor(
      RoutingService routingService, List<TransitMode> filterByModes,
      List<PlaceType> filterByPlaceTypes, List<FeedScopedId> filterByStops,
      List<FeedScopedId> filterByRoutes, List<String> filterByBikeRentalStations, int maxResults,
      double radiusMeters
  ) {
    this.routingService = routingService;
    this.filterByModes = toSet(filterByModes);
    this.filterByStops = toSet(filterByStops);
    this.filterByRoutes = toSet(filterByRoutes);
    this.filterByBikeRentalStation = toSet(filterByBikeRentalStations);

    includeStops = filterByPlaceTypes == null || filterByPlaceTypes.contains(PlaceType.STOP);
    includePatternAtStops = filterByPlaceTypes == null
        || filterByPlaceTypes.contains(PlaceType.PATTERN_AT_STOP);
    includeBikeShares = filterByPlaceTypes == null
        || filterByPlaceTypes.contains(PlaceType.BICYCLE_RENT);
    this.maxResults = maxResults;
    this.radiusMeters = radiusMeters;
  }

  private static <T> Set<T> toSet(List<T> list) {
    if (list == null) { return null; }
    return Set.copyOf(list);
  }

  private boolean stopHasRoutesWithMode(Stop stop, Set<TransitMode> modes) {
    return routingService
        .getPatternsForStop(stop)
        .stream()
        .map(TripPattern::getMode)
        .anyMatch(modes::contains);
  }

  @Override
  public void visitEdge(Edge edge, State state) { }

  @Override
  public void visitEnqueue(State state) { }

  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    double distance = state.getWalkDistance();
    if (vertex instanceof TransitStopVertex) {
      Stop stop = ((TransitStopVertex) vertex).getStop();
      handleStop(stop, distance);
      handlePatternsAtStop(stop, distance);
    }
    else if (vertex instanceof VehicleRentalStationVertex) {
      handleBikeRentalStation(((VehicleRentalStationVertex) vertex).getStation(), distance);
    }
  }

  private void handleStop(Stop stop, double distance) {
    if (filterByStops != null && !filterByStops.contains(stop.getId())) { return; }
    if (includeStops && !seenStops.contains(stop.getId()) && (
        filterByModes == null || stopHasRoutesWithMode(stop, filterByModes)
    )) {
      placesFound.add(new PlaceAtDistance(stop, distance));
      seenStops.add(stop.getId());
    }
  }

  private void handlePatternsAtStop(Stop stop, double distance) {
    if (includePatternAtStops) {
      List<TripPattern> patterns = routingService
          .getPatternsForStop(stop)
          .stream()
          .filter(pattern -> filterByModes == null || filterByModes.contains(pattern.getMode()))
          .filter(pattern -> filterByRoutes == null
              || filterByRoutes.contains(pattern.getRoute().getId()))
          .filter(pattern -> pattern.canBoard(stop))
          .collect(toList());

      for (TripPattern pattern : patterns) {
        String seenKey = pattern.getRoute().getId().toString() + ":" + pattern.getId().toString();
        if (!seenPatternAtStops.contains(seenKey)) {
          PatternAtStop row = new PatternAtStop(stop, pattern);
          PlaceAtDistance place = new PlaceAtDistance(row, distance);
          placesFound.add(place);
          seenPatternAtStops.add(seenKey);
        }
      }
    }
  }

  private void handleBikeRentalStation(VehicleRentalPlace station, double distance) {
    if (!includeBikeShares) { return; }
    if (filterByBikeRentalStation != null && !filterByBikeRentalStation.contains(station.getStationId())) {
      return;
    }
    if (seenBicycleRentalStations.contains(station.getId())) { return; }
    seenBicycleRentalStations.add(station.getId());
    placesFound.add(new PlaceAtDistance(station, distance));
  }

  /**
   * @return A SkipEdgeStrategy to be used with this TraverseVisitor. It skips edges when either
   *          the maximum number of places or the furthest distance has been reached. However,
   *          when the maximum number of places has been reached, it continues searching along
   *          other paths until the distance of the place that is furthest away. This is to account
   *          for the fact that the a star does not traverse edges ordered by distance.
   */
  public SkipEdgeStrategy getSkipEdgeStrategy() {

    return (origin, target, current, edge, spt, traverseOptions) -> {

      double furthestDistance = radiusMeters;

      if (PlaceFinderTraverseVisitor.this.placesFound.size()
          >= PlaceFinderTraverseVisitor.this.maxResults) {
        furthestDistance = 0;
        for (PlaceAtDistance pad : PlaceFinderTraverseVisitor.this.placesFound) {
          if (pad.distance > furthestDistance) {
            furthestDistance = pad.distance;
          }
        }
      }

      return current.getWalkDistance() > furthestDistance;
    };
  }
}
