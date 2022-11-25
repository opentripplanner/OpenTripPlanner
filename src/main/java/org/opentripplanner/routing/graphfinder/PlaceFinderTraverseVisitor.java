package org.opentripplanner.routing.graphfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

/**
 * A TraverseVisitor used in finding various types of places while walking the street graph.
 */
public class PlaceFinderTraverseVisitor implements TraverseVisitor<State, Edge> {

  public final List<PlaceAtDistance> placesFound = new ArrayList<>();
  private final TransitService transitService;
  private final Set<TransitMode> filterByModes;
  private final Set<FeedScopedId> filterByStops;
  private final Set<FeedScopedId> filterByRoutes;
  private final Set<String> filterByVehicleRental;
  private final Set<String> seenPatternAtStops = new HashSet<>();
  private final Set<FeedScopedId> seenStops = new HashSet<>();
  private final Set<FeedScopedId> seenVehicleRentalPlaces = new HashSet<>();
  private final Set<FeedScopedId> seenParkingLots = new HashSet<>();
  private final boolean includeStops;
  private final boolean includePatternAtStops;
  private final boolean includeVehicleRentals;
  private final boolean includeCarParking;
  private final boolean includeBikeParking;
  private final int maxResults;
  private final double radiusMeters;

  /**
   * @param transitService             A TransitService used in finding information about the
   *                                   various places.
   * @param filterByModes              A list of TransitModes for which to find Stops and
   *                                   PatternAtStops. Use null to disable the filtering.
   * @param filterByPlaceTypes         A list of PlaceTypes to search for. Use null to disable the
   *                                   filtering, and search for all types.
   * @param filterByStops              A list of Stop ids for which to find Stops and
   *                                   PatternAtStops. Use null to disable the filtering.
   * @param filterByRoutes             A list of Route ids used for filtering Stops. Only the stops
   *                                   which are served by the route are returned. Use null to
   *                                   disable the filtering.
   * @param filterByBikeRentalStations A list of VehicleRentalStation ids to use in filtering.  Use
   *                                   null to disable the filtering.
   * @param maxResults                 Maximum number of results to return.
   */
  public PlaceFinderTraverseVisitor(
    TransitService transitService,
    List<TransitMode> filterByModes,
    List<PlaceType> filterByPlaceTypes,
    List<FeedScopedId> filterByStops,
    List<FeedScopedId> filterByRoutes,
    List<String> filterByBikeRentalStations,
    int maxResults,
    double radiusMeters
  ) {
    this.transitService = transitService;
    this.filterByModes = toSet(filterByModes);
    this.filterByStops = toSet(filterByStops);
    this.filterByRoutes = toSet(filterByRoutes);
    this.filterByVehicleRental = toSet(filterByBikeRentalStations);

    includeStops = shouldInclude(filterByPlaceTypes, PlaceType.STOP);
    includePatternAtStops = shouldInclude(filterByPlaceTypes, PlaceType.PATTERN_AT_STOP);
    includeVehicleRentals = shouldInclude(filterByPlaceTypes, PlaceType.VEHICLE_RENT);
    includeCarParking = shouldInclude(filterByPlaceTypes, PlaceType.CAR_PARK);
    includeBikeParking = shouldInclude(filterByPlaceTypes, PlaceType.BIKE_PARK);
    this.maxResults = maxResults;
    this.radiusMeters = radiusMeters;
  }

  @Override
  public void visitEdge(Edge edge) {}

  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    double distance = state.getWalkDistance();
    if (vertex instanceof TransitStopVertex transitVertex) {
      RegularStop stop = transitVertex.getStop();
      handleStop(stop, distance);
      handlePatternsAtStop(stop, distance);
    } else if (vertex instanceof VehicleRentalPlaceVertex rentalVertex) {
      handleVehicleRental(rentalVertex.getStation(), distance);
    } else if (vertex instanceof VehicleParkingEntranceVertex parkingVertex) {
      handleParking(parkingVertex.getVehicleParking(), distance);
    }
  }

  @Override
  public void visitEnqueue() {}

  /**
   * @return A SkipEdgeStrategy to be used with this TraverseVisitor. It skips edges when either the
   * maximum number of places or the furthest distance has been reached. However, when the maximum
   * number of places has been reached, it continues searching along other paths until the distance
   * of the place that is furthest away. This is to account for the fact that the a star does not
   * traverse edges ordered by distance.
   */
  public SkipEdgeStrategy<State, Edge> getSkipEdgeStrategy() {
    return (current, edge) -> {
      double furthestDistance = radiusMeters;

      if (
        PlaceFinderTraverseVisitor.this.placesFound.size() >=
        PlaceFinderTraverseVisitor.this.maxResults
      ) {
        furthestDistance = 0;
        for (PlaceAtDistance pad : PlaceFinderTraverseVisitor.this.placesFound) {
          if (pad.distance() > furthestDistance) {
            furthestDistance = pad.distance();
          }
        }
      }

      return current.getWalkDistance() > furthestDistance;
    };
  }

  private static <T> Set<T> toSet(List<T> list) {
    if (list == null) {
      return null;
    }
    return Set.copyOf(list);
  }

  private void handleParking(VehicleParking parking, double distance) {
    if (!seenParkingLots.contains(parking.getId())) {
      if (includeBikeParking && parking.hasBicyclePlaces()) {
        placesFound.add(new PlaceAtDistance(parking, distance));
        seenParkingLots.add(parking.getId());
      }
      // make sure that we don't add the same place twice if it has bike and car parking spaces
      if (
        includeCarParking && parking.hasAnyCarPlaces() && !seenParkingLots.contains(parking.getId())
      ) {
        placesFound.add(new PlaceAtDistance(parking, distance));
        seenParkingLots.add(parking.getId());
      }
    }
  }

  private boolean shouldInclude(List<PlaceType> filterByPlaceTypes, PlaceType type) {
    return filterByPlaceTypes == null || filterByPlaceTypes.contains(type);
  }

  private boolean stopHasPatternsWithMode(RegularStop stop, Set<TransitMode> modes) {
    return transitService
      .getPatternsForStop(stop)
      .stream()
      .map(TripPattern::getMode)
      .anyMatch(modes::contains);
  }

  private void handleStop(RegularStop stop, double distance) {
    if (filterByStops != null && !filterByStops.contains(stop.getId())) {
      return;
    }
    if (
      includeStops &&
      !seenStops.contains(stop.getId()) &&
      (filterByModes == null || stopHasPatternsWithMode(stop, filterByModes))
    ) {
      placesFound.add(new PlaceAtDistance(stop, distance));
      seenStops.add(stop.getId());
    }
  }

  private void handlePatternsAtStop(RegularStop stop, double distance) {
    if (includePatternAtStops) {
      List<TripPattern> patterns = transitService
        .getPatternsForStop(stop)
        .stream()
        .filter(pattern -> filterByModes == null || filterByModes.contains(pattern.getMode()))
        .filter(pattern ->
          filterByRoutes == null || filterByRoutes.contains(pattern.getRoute().getId())
        )
        .filter(pattern -> pattern.canBoard(stop))
        .toList();

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

  private void handleVehicleRental(VehicleRentalPlace station, double distance) {
    if (!includeVehicleRentals) {
      return;
    }
    if (filterByVehicleRental != null && !filterByVehicleRental.contains(station.getStationId())) {
      return;
    }
    if (seenVehicleRentalPlaces.contains(station.getId())) {
      return;
    }
    seenVehicleRentalPlaces.add(station.getId());
    placesFound.add(new PlaceAtDistance(station, distance));
  }
}
