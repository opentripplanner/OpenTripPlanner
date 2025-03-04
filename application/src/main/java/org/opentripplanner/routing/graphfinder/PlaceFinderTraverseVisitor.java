package org.opentripplanner.routing.graphfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
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
  private final Set<FeedScopedId> filterByStations;
  private final Set<FeedScopedId> filterByRoutes;
  private final Set<String> filterByNetwork;
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
  private final boolean includeStations;
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
    List<FeedScopedId> filterByStations,
    List<FeedScopedId> filterByRoutes,
    List<String> filterByBikeRentalStations,
    List<String> filterByNetwork,
    int maxResults,
    double radiusMeters
  ) {
    if (filterByPlaceTypes == null || filterByPlaceTypes.isEmpty()) {
      throw new IllegalArgumentException("No place type filter was included in request");
    }
    this.transitService = transitService;

    this.filterByModes = toSet(filterByModes);
    this.filterByStops = toSet(filterByStops);
    this.filterByStations = toSet(filterByStations);
    this.filterByRoutes = toSet(filterByRoutes);
    this.filterByVehicleRental = toSet(filterByBikeRentalStations);
    this.filterByNetwork = toSet(filterByNetwork);
    includeStops = shouldInclude(filterByPlaceTypes, PlaceType.STOP);

    includePatternAtStops = shouldInclude(filterByPlaceTypes, PlaceType.PATTERN_AT_STOP);
    includeVehicleRentals = shouldInclude(filterByPlaceTypes, PlaceType.VEHICLE_RENT);
    includeCarParking = shouldInclude(filterByPlaceTypes, PlaceType.CAR_PARK);
    includeBikeParking = shouldInclude(filterByPlaceTypes, PlaceType.BIKE_PARK);
    includeStations = shouldInclude(filterByPlaceTypes, PlaceType.STATION);
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
      return Set.of();
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
    return filterByPlaceTypes.contains(type);
  }

  private boolean stopHasPatternsWithMode(RegularStop stop, Set<TransitMode> modes) {
    return transitService
      .findPatterns(stop)
      .stream()
      .map(TripPattern::getMode)
      .anyMatch(modes::contains);
  }

  private boolean stopIsIncludedByStopFilter(RegularStop stop) {
    return filterByStops.isEmpty() || filterByStops.contains(stop.getId());
  }

  private boolean stopIsIncludedByStationFilter(RegularStop stop) {
    return (
      ((filterByStations.isEmpty() || filterByStations.contains(stop.getParentStation().getId())))
    );
  }

  private boolean stopIsIncludedByModeFilter(RegularStop stop) {
    return filterByModes.isEmpty() || stopHasPatternsWithMode(stop, filterByModes);
  }

  /* Checks whether the stop is included in the stop filter and whether the stop should be considered
   * a stop or a station in the search.*/
  private boolean stopShouldNotBeIncludedAsStop(RegularStop stop) {
    return (
      (includeStations && !stop.isPartOfStation() && !stopIsIncludedByStopFilter(stop)) ||
      (!includeStations && !stopIsIncludedByStopFilter(stop))
    );
  }

  /* Checks if the stop is a part of a station and whether that station is
   * included in the station filter */
  private boolean stopShouldNotBeIncludedAsStation(RegularStop stop) {
    return stop.isPartOfStation() && !stopIsIncludedByStationFilter(stop);
  }

  private void handleStop(RegularStop stop, double distance) {
    // Do not consider stop if it is not included in the stop or mode filter
    // or if it or its parent station has already been seen.
    if (
      stopShouldNotBeIncludedAsStop(stop) ||
      stopShouldNotBeIncludedAsStation(stop) ||
      seenStops.contains(stop.getId()) ||
      seenStops.contains(stop.getStationOrStopId()) ||
      !stopIsIncludedByModeFilter(stop)
    ) {
      return;
    }

    if (includeStations && stop.getParentStation() != null) {
      seenStops.add(stop.getParentStation().getId());
      placesFound.add(new PlaceAtDistance(stop.getParentStation(), distance));
    } else if (includeStops) {
      seenStops.add(stop.getId());
      placesFound.add(new PlaceAtDistance(stop, distance));
    }
  }

  private void handlePatternsAtStop(RegularStop stop, double distance) {
    if (includePatternAtStops) {
      List<TripPattern> patterns = transitService
        .findPatterns(stop)
        .stream()
        .filter(pattern -> filterByModes.isEmpty() || filterByModes.contains(pattern.getMode()))
        .filter(
          pattern -> filterByRoutes.isEmpty() || filterByRoutes.contains(pattern.getRoute().getId())
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
    if (
      !filterByVehicleRental.isEmpty() && !filterByVehicleRental.contains(station.getStationId())
    ) {
      return;
    }
    if (seenVehicleRentalPlaces.contains(station.getId())) {
      return;
    }
    if (!filterByNetwork.isEmpty() && !filterByNetwork.contains(station.getNetwork())) {
      return;
    }
    seenVehicleRentalPlaces.add(station.getId());
    placesFound.add(new PlaceAtDistance(station, distance));
  }
}
