package org.opentripplanner.street.api.path;

import static org.opentripplanner.street.api.path.ElevationProfileEncoder.encodeElevationProfileWithNaN;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.search.state.State;

/// A [StreetPath] backed by the states of a street search.
final class DefaultStreetPath implements StreetPath {

  private final List<State> states;
  private final List<Edge> edges;

  DefaultStreetPath(List<State> states, List<Edge> edges) {
    validate(states, edges);
    this.states = states;
    this.edges = edges;
  }

  /**
   * Build a chronologically ordered path by following the back-state chain of {@code finalState}
   * all the way back to the origin of the search. When {@code finalState} comes from an arriveBy
   * search, the chain is reversed first, since the back-state chain otherwise runs the "wrong" way
   * for that search direction.
   */
  DefaultStreetPath(State finalState) {
    var state = finalState.getRequest().arriveBy() ? finalState.reverse() : finalState;

    List<State> states = new ArrayList<>();
    for (State s : state.listBackStates()) {
      states.add(s);
    }
    Collections.reverse(states);

    List<Edge> edges = new ArrayList<>(states.size() - 1);
    for (int i = 1; i < states.size(); i++) {
      edges.add(states.get(i).getBackEdge());
    }

    validate(states, edges);
    this.states = states;
    this.edges = edges;
  }

  private static void validate(List<State> states, List<Edge> edges) {
    if (states.isEmpty()) {
      throw new IllegalArgumentException("A path needs at least one state");
    }
    if (edges.size() != states.size() - 1) {
      throw new IllegalArgumentException("A path needs an edge between each state");
    }
  }

  @Override
  public Cost generalizedCost() {
    return Cost.costOfSeconds(weight());
  }

  /// The duration is computed from the millisecond precision times of the first and last state
  /// and rounded up, the same way as [State#getElapsedTimeSeconds()].
  @Override
  public Duration duration() {
    long millis = Math.abs(
      states.getLast().getTimeMilliseconds() - states.getFirst().getTimeMilliseconds()
    );
    return Duration.ofSeconds((millis + 999L) / 1000L);
  }

  @Override
  public ElevationChange elevationChange() {
    return calculateElevations();
  }

  @Override
  public boolean arrivesWithRentedVehicleFromStation() {
    return lastState().isRentingVehicleFromStation();
  }

  @Override
  public List<StreetLeg> computeLegs(double ellipsoidToGeoidDifference) {
    return new StreetPathToLegsMapper(ellipsoidToGeoidDifference).map(this);
  }

  /// The start of the path in seconds
  Instant startTime() {
    return states.getFirst().getTime();
  }

  /// The end of the path in seconds
  Instant endTime() {
    return states.getLast().getTime();
  }

  double weight() {
    return states.getLast().weight - states.getFirst().weight;
  }

  double distanceMeters() {
    return edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
  }

  LineString geometry() {
    var geometries = edges
      .stream()
      .filter(Edge::includeGeometryInPath)
      .map(Edge::getGeometry)
      .filter(Objects::nonNull);

    return GeometryUtils.concatenateLineStrings(geometries::iterator);
  }

  /// Get all the states of this path
  List<State> states() {
    return states;
  }

  /// Get the last state in the path
  State lastState() {
    return states.getLast();
  }

  ElevationProfile elevation(boolean geoidElevation, double ellipsoidToGeoidDifference) {
    var builder = ElevationProfile.of();

    double heightOffset = geoidElevation ? ellipsoidToGeoidDifference : 0;

    double distanceOffset = 0;
    for (final Edge edge : edges) {
      if (edge.getDistanceMeters() > 0) {
        builder.add(encodeElevationProfileWithNaN(edge, distanceOffset, heightOffset));
        distanceOffset += edge.getDistanceMeters();
      }
    }

    var p = builder.build();

    return p.isAllYUnknown() ? null : p;
  }

  /// Calculate the elevationGained and elevationLost
  ElevationChange calculateElevations() {
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
          elevationLost_m -= change_m;
        }
      }
    }
    return new ElevationChange(elevationGained_m, elevationLost_m);
  }

  /// Get a specific section of this path as a new path.
  ///
  /// @param startIdx the first state index (inclusive)
  /// @param endIdx the end state index (exclusive)
  DefaultStreetPath subPath(int startIdx, int endIdx) {
    var subStates = states.subList(startIdx, endIdx);
    var subEdges = edges.subList(startIdx, endIdx - 1);
    return new DefaultStreetPath(subStates, subEdges);
  }
}
