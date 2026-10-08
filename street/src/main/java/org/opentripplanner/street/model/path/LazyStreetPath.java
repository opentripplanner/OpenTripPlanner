package org.opentripplanner.street.model.path;

import com.google.common.collect.Iterables;
import java.time.Duration;
import java.time.Instant;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

public class LazyStreetPath {

  private final State finalState;

  public LazyStreetPath(State finalState) {
    this.finalState = finalState;
  }

  /// We are trying to make the State internal to the street module. Please don't use this for any new code. This getter
  /// will be removed in the future.
  @Deprecated
  public State getFinalState() {
    return finalState;
  }

  /// The weight of the path
  public double getWeight() {
    return finalState.getWeight();
  }

  /// How long the path took in seconds
  public long getElapsedTimeSeconds() {
    return finalState.getElapsedTimeSeconds();
  }

  public double getTraversalDistanceMeters() {
    return finalState.getTraversalDistanceMeters();
  }

  /// How long the path took as a duration
  public Duration getDuration() {
    return Duration.ofSeconds(finalState.getElapsedTimeSeconds());
  }

  /// The chronological start time of the path
  public Instant startTime() {
    return isReverse() ? destinationTime() : initialTime();
  }

  /// The chronological end time of the path
  public Instant endTime() {
    return isReverse() ? initialTime() : destinationTime();
  }

  /// The time at the path origin. Note: for reverse searches this will be after the destnation time.
  public Instant initialTime() {
    return finalState.getRequest().startTime();
  }

  /// The time at the path destination. Note: for reverse searches this will be after the destnation time.
  public Instant destinationTime() {
    return finalState.getTime();
  }

  /// Whether this was an arriveBy search. If true, the path destination is at the chronological start of the path.
  public boolean isReverse() {
    return finalState.getRequest().arriveBy();
  }

  /// This might traverse all the edges
  public boolean hasAnyCar() {
    return finalState.containsModeCar();
  }

  /// Whether we are renting a vehicle at the destination. Note The destination will be the chonological start of the
  /// path for reverse searches!
  public boolean destinationIsRentingVehicleFromStation() {
    return finalState.isRentingVehicleFromStation();
  }

  public LineString getGeometry() {
    if (isReverse()) {
      var geometries = Iterables.transform(finalState.listBackEdges(), Edge::getGeometry);
      return GeometryUtils.concatenateLineStrings(geometries);
    } else {
      // TODO: It seems wasteful that we reverse the linestrings twice here instead of reversing the iterator
      var geometries = Iterables.transform(finalState.listBackEdges(), LazyStreetPath::reverse);
      return GeometryUtils.concatenateLineStrings(geometries).reverse();
    }
  }

  /// Returns an efficient iterable that allows traversing the edge chain backwards.
  public Iterable<Edge> listBackEdges() {
    return finalState.listBackEdges();
  }

  /// Returns a materialized non-lazy StreetPath
  public StreetPath materialize() {
    return new StreetPath(finalState);
  }

  private static LineString reverse(Edge e) {
    var geom = e.getGeometry();
    if (geom == null) {
      return null;
    } else {
      return geom.reverse();
    }
  }
}
