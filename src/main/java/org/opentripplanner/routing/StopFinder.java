package org.opentripplanner.routing;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.astar.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class StopFinder {

  /* TODO: an almost similar function exists in ProfileRouter, combine these.
   *  Should these live in a separate class? */
  public static void findClosestByWalking(
      Graph graph, double lat, double lon, int radius, TraverseVisitor visitor, SearchTerminationStrategy terminationStrategy
  ) {
    // Make a normal OTP routing request so we can traverse edges and use GenericAStar
    // TODO make a function that builds normal routing requests from profile requests
    RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
    rr.from = new GenericLocation(null, null, lat, lon);
    rr.oneToMany = true;
    rr.setRoutingContext(graph);
    rr.walkSpeed = 1;
    rr.dominanceFunction = new DominanceFunction.LeastWalk();
    rr.rctx.remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
    // RR dateTime defaults to currentTime.
    // If elapsed time is not capped, searches are very slow.
    rr.worstTime = (rr.dateTime + radius);
    AStar astar = new AStar();
    rr.setNumItineraries(1);
    astar.setTraverseVisitor(visitor);
    astar.getShortestPathTree(rr, 1, terminationStrategy); // timeout in seconds
    // Destroy the routing context, to clean up the temporary edges & vertices
    rr.rctx.destroy();
  }

  public static class StopAndDistance {

    public Stop stop;
    public int distance;

    public StopAndDistance(Stop stop, int distance) {
      this.stop = stop;
      this.distance = distance;
    }
  }

  static class StopFinderTraverseVisitor implements TraverseVisitor {

    List<StopAndDistance> stopsFound = new ArrayList<>();

    @Override
    public void visitEdge(Edge edge, State state) { }

    @Override
    public void visitEnqueue(State state) { }

    // Accumulate stops into ret as the search runs.
    @Override
    public void visitVertex(State state) {
      Vertex vertex = state.getVertex();
      if (vertex instanceof TransitStopVertex) {
        stopsFound.add(new StopAndDistance(((TransitStopVertex) vertex).getStop(),
            (int) state.getElapsedTimeSeconds()
        ));
      }
    }
  }

  public enum PlaceType {
    STOP, DEPARTURE_ROW, BICYCLE_RENT, BIKE_PARK, CAR_PARK;
  }

  public static class PlaceAndDistance {
    public Object place;
    public int distance;

    public PlaceAndDistance(Object place, int distance) {
      this.place = place;
      this.distance = distance;
    }
  }

  public static class DepartureRow {
    public String id;
    public Stop stop;
    public TripPattern pattern;

    public DepartureRow(Stop stop, TripPattern pattern) {
      this.id = toId(stop, pattern);
      this.stop = stop;
      this.pattern = pattern;
    }

    private static String toId(Stop stop, TripPattern pattern) {
      return stop.getId() + ";" + pattern.getId();
    }

    public List<TripTimeShort> getStoptimes(
        RoutingService routingService,
        long startTime,
        int timeRange,
        int numberOfDepartures,
        boolean omitNonPickups,
        boolean omitCanceled)
    {
      return Collections.EMPTY_LIST;
      // TODO
      // return routingService.stopTimesForPattern(stop, pattern, startTime, timeRange, numberOfDepartures, omitNonPickups, omitCanceled);
    }

    public static DepartureRow fromId(RoutingService routingService, String id) {
      String[] parts = id.split(";", 2);
      FeedScopedId stopId = FeedScopedId.parseId(parts[0]);
      FeedScopedId patternId = FeedScopedId.parseId(parts[1]);
      return new DepartureRow(
          routingService.getStopForId(stopId),
          routingService.getTripPatternForId(patternId)
      );
    }
  }

  // TODO Add car and bike parks
  public static class PlaceFinderTraverseVisitor implements TraverseVisitor {
    public List<PlaceAndDistance> placesFound = new ArrayList<>();
    private RoutingService routingService;
    private Set<TransitMode> filterByModes;
    private Set<FeedScopedId> filterByStops;
    private Set<FeedScopedId> filterByRoutes;
    private Set<String> filterByBikeRentalStation;
    private Set<String> seenDepartureRows = new HashSet<>();
    private Set<FeedScopedId> seenStops = new HashSet<>();
    private Set<String> seenBicycleRentalStations = new HashSet<>();
    private boolean includeStops;
    private boolean includeDepartureRows;
    private boolean includeBikeShares;
    private int maxResults;
    private PlaceFinderSearchTerminationStrategy searchTerminationStrategy;

    public PlaceFinderTraverseVisitor(
        RoutingService routingService,
        List<TransitMode> filterByModes,
        List<PlaceType> filterByPlaceTypes,
        List<FeedScopedId> filterByStops,
        List<FeedScopedId> filterByRoutes,
        List<String> filterByBikeRentalStations,
        int maxResults
    ) {
      this.routingService = routingService;
      this.filterByModes = toSet(filterByModes);
      this.filterByStops = toSet(filterByStops);
      this.filterByRoutes = toSet(filterByRoutes);
      this.filterByBikeRentalStation = toSet(filterByBikeRentalStations);

      includeStops = filterByPlaceTypes == null || filterByPlaceTypes.contains(PlaceType.STOP);
      includeDepartureRows = filterByPlaceTypes == null || filterByPlaceTypes.contains(PlaceType.DEPARTURE_ROW);
      includeBikeShares = filterByPlaceTypes == null || filterByPlaceTypes.contains(PlaceType.BICYCLE_RENT);
      this.maxResults = maxResults;
    }

    private static <T> Set<T> toSet(List<T> list) {
      if (list == null) return null;
      return new HashSet<T>(list);
    }

    private boolean stopHasRoutesWithMode(Stop stop, Set<TransitMode> modes) {
      return routingService
          .getPatternsForStop(stop)
          .stream()
          .map(TripPattern::getMode)
          .anyMatch(modes::contains);
    }

    @Override public void visitEdge(Edge edge, State state) { }

    @Override public void visitEnqueue(State state) { }

    @Override public void visitVertex(State state) {
      Vertex vertex = state.getVertex();
      int distance = (int)state.getWalkDistance();
      if (vertex instanceof TransitStopVertex) {
        Stop stop = ((TransitStopVertex)vertex).getStop();
        handleStop(stop, distance);
        handleDepartureRows(stop, distance);
      } else if (vertex instanceof BikeRentalStationVertex) {
        handleBikeRentalStation(((BikeRentalStationVertex)vertex).getStation(), distance);
      }
    }

    private void handleStop(Stop stop, int distance) {
      if (filterByStops != null && !filterByStops.contains(stop.getId())) return;
      if (includeStops
          && !seenStops.contains(stop.getId())
          && (filterByModes == null || stopHasRoutesWithMode(stop, filterByModes))
      ) {
        placesFound.add(new PlaceAndDistance(stop, distance));
        seenStops.add(stop.getId());
      }
    }

    private void handleDepartureRows(Stop stop, int distance) {
      if (includeDepartureRows) {
        List<TripPattern> patterns = routingService.getPatternsForStop(stop)
            .stream()
            .filter(pattern -> filterByModes == null || filterByModes.contains(pattern.getMode()))
            .filter(pattern -> filterByRoutes == null || filterByRoutes.contains(pattern.route.getId()))
            .filter(pattern -> pattern.canBoard(pattern.getStopIndex(stop)))
            .collect(toList());

        for (TripPattern pattern : patterns) {
          String seenKey = pattern.route.getId().toString() + ":" + pattern.getId().toString();
          if (!seenDepartureRows.contains(seenKey)) {
            DepartureRow row = new DepartureRow(stop, pattern);
            PlaceAndDistance place = new PlaceAndDistance(row, distance);
            placesFound.add(place);
            seenDepartureRows.add(seenKey);
          }
        }
      }
    }

    private void handleBikeRentalStation(BikeRentalStation station, int distance) {
      if (!includeBikeShares) return;
      if (filterByBikeRentalStation != null && !filterByBikeRentalStation.contains(station.id)) return;
      if (seenBicycleRentalStations.contains(station.id)) return;
      seenBicycleRentalStations.add(station.id);
      placesFound.add(new PlaceAndDistance(station, distance));
    }

    public PlaceFinderSearchTerminationStrategy getSearchTerminationStrategy() {
      if (this.searchTerminationStrategy == null) {
        this.searchTerminationStrategy = new PlaceFinderSearchTerminationStrategy();
      }
      return this.searchTerminationStrategy;
    }

    class PlaceFinderSearchTerminationStrategy implements SearchTerminationStrategy {
        @Override
        public boolean shouldSearchTerminate(
            Set<Vertex> origin,
            Set<Vertex> target,
            State current,
            ShortestPathTree spt,
            RoutingRequest traverseOptions
        ) {
          // the first n stops the search visit may not be the nearest n
          // but when we have at least n stops found, we can update the
          // max distance to be the furthest of the places so far
          // and let the search terminate at that distance
          // and then return the first n
          if (PlaceFinderTraverseVisitor.this.placesFound.size() >= PlaceFinderTraverseVisitor.this.maxResults) {
            int furthestDistance = 0;
            for (PlaceAndDistance pad : PlaceFinderTraverseVisitor.this.placesFound) {
              if (pad.distance > furthestDistance) {
                furthestDistance = pad.distance;
              }
            }
            traverseOptions.worstTime = (traverseOptions.dateTime + furthestDistance);
          }
          return false;

      }
    }
  }
}
