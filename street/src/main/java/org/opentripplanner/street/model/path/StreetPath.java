package org.opentripplanner.street.model.path;

import static org.opentripplanner.street.model.path.ElevationProfileEncoder.encodeElevationProfileWithNaN;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.elevation.ElevationProfile;
import org.opentripplanner.street.search.state.State;

/// This class represents a path within the street network
public class StreetPath {

  private final List<State> states;
  private final List<Edge> edges;

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

  /**
   * Build a chronologically ordered path by following the back-state chain of {@code finalState}
   * all the way back to the origin of the search. When {@code finalState} comes from an arriveBy
   * search, the chain is reversed first, since the back-state chain otherwise runs the "wrong"
   * way for that search direction.
   */
  public StreetPath(State finalState) {
    var state = finalState.getRequest().arriveBy() ? finalState.reverse() : finalState;
    var states = reversedList(state.listBackStates());
    var edges = reversedList(state.listBackEdges());
    this(states, edges);
  }

  private static <T> List<T> reversedList(Iterable<T> backIterable) {
    List<T> list = new ArrayList<>();
    for (T t : backIterable) {
      list.add(t);
    }
    Collections.reverse(list);
    return list;
  }

  /// The start of the path in seconds
  public Instant startTime() {
    return states.getFirst().getTime();
  }

  /// The end of the path in seconds
  public Instant endTime() {
    return states.getLast().getTime();
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
    var geometries = edges
      .stream()
      .filter(Edge::includeGeometryInPath)
      .map(Edge::getGeometry)
      .filter(Objects::nonNull);

    return GeometryUtils.concatenateLineStrings(geometries::iterator);
  }

  /// Get all the states of this path
  public List<State> states() {
    return states;
  }

  /// Get the last state in the path
  public State lastState() {
    return states.getLast();
  }

  public ElevationProfile elevation(boolean geoidElevation, double ellipsoidToGeoidDifference) {
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
  public StreetPath subPath(int startIdx, int endIdx) {
    var subStates = states.subList(startIdx, endIdx);
    var subEdges = edges.subList(startIdx, endIdx - 1);
    return new StreetPath(subStates, subEdges);
  }
}
