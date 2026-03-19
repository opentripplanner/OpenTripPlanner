package org.opentripplanner.street.search;

import static org.opentripplanner.street.search.state.VehicleRentalState.RENTING_FLOATING;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/// This class represents a path within the street network
public class StreetPath {

  private final List<State> states;
  private final List<Edge> edges;

  public StreetPath(GraphPath<State, Edge, Vertex> path) {
    this(path.states, path.edges);
  }

  public StreetPath(List<State> states, List<Edge> edges) {
    if (states.isEmpty()) {
      throw new IllegalArgumentException("A path needs at least one state");
    }
    if (edges.size() != states.size() - 1) {
      throw new IllegalArgumentException("A path needs an edge between each state");
    }
    this.states = states;
    this.edges = edges;
  }

  public StreetPath(State endState) {
    this(new GraphPath<>(endState));
  }

  /// The start of the path in seconds
  public Instant startTime() {
    return states.getFirst().getTime();
  }

  /// The end of the path in seconds
  public Instant endTime() {
    return states.getLast().getTime();
  }

  /// The start of the path in milliseconds
  public Instant startTimeAccurate() {
    return states.getFirst().getTimeAccurate();
  }

  /// The end of the path in milliseconds
  public Instant endTimeAccurate() {
    return states.getLast().getTimeAccurate();
  }

  public double weight() {
    return states.getLast().weight - states.getFirst().weight;
  }

  public double distanceMeters() {
    return edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
  }

  /// The duration of the trip in seconds
  public Duration duration() {
    return startTime().until(endTime());
  }

  public LineString geometry() {
    var geometries = edges.stream()
      .filter(e -> !e.excludeGeometryFromLeg())
      .map(Edge::getGeometry)
      .filter(Objects::nonNull)
      .toList();

    return GeometryUtils.concatenateLineStrings(geometries);
  }

  /// Whether the path ends with renting a vehicle from a station
  public boolean endsWithRentingVehicleFromStation() {
    return states.getLast().isRentingVehicleFromStation();
  }

  /// Get all the states of this path
  public List<State> states() {
    return states;
  }


  /// Get the last state in the patn
  public State lastState() {
    return states.getLast();
  }

  /// Get all the states of this path
  public List<Edge> edges() {
    return edges;
  }

  /// Calculate the elevationGained and elevationLost
  public ElevationChange calculateElevations() {
    double elevationGained_m = 0.0;
    double elevationLost_m = 0.0;
    for (Edge edge : edges) {
      if (!(edge instanceof StreetEdge edgeWithElevation)) {
        continue;
      }
      PackedCoordinateSequence coordinates = edgeWithElevation.getElevationProfile();

      if (coordinates == null) {
        continue;
      }
      // TODO Check the test below, AFAIU current elevation profile has 3 dimensions.
      if (coordinates.getDimension() != 2) {
        continue;
      }

      for (int i = 0; i < coordinates.size() - 1; i++) {
        double change_m = coordinates.getOrdinate(i + 1, 1) - coordinates.getOrdinate(i, 1);
        if (change_m > 0.0) {
          elevationGained_m += change_m;
        } else {
          elevationLost_m += change_m;
        }
      }
    }
    return new ElevationChange(elevationGained_m, elevationLost_m);
  }

  public StreetPath subPath(int startIdx, int endIdx) {
    var subStates = states.subList(startIdx, endIdx);
    var subEdges = edges.subList(startIdx, endIdx - 1);
    return new StreetPath(subStates, subEdges);
  }

  /**
   * Dropping of a free-floating vehicle can happen at any edge so be sure to select the correct
   * state (forward, not backward).
   */
  private static boolean isFloatingRentalDropoff(State state) {
    return (
      !state.isRentingVehicle() &&
      (state.getBackState() != null &&
        state.getBackState().getVehicleRentalState() == RENTING_FLOATING)
    );
  }

  /// This is only used in the carpooling code and can be removed once the carpooling migrates
  /// to use the StreetPath instead
  @Deprecated
  public GraphPath<State, Edge, Vertex> toGraphPath() {
    return new GraphPath<>(states, edges);
  }
}
