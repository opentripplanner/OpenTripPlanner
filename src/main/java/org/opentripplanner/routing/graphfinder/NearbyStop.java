package org.opentripplanner.routing.graphfinder;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A specific stop at a distance. Also includes a geometry and potentially a list of edges and a
 * state of how to reach the stop from the search origin
 */
public class NearbyStop implements Comparable<NearbyStop> {

  private static GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

  public final StopLocation stop;
  public final double distance;
  public final int distanceIndependentTime;

  public final List<Edge> edges;
  public final LineString geometry;
  public final State state;

  public NearbyStop(
      StopLocation stop, double distance, int distanceIndependentTime, List<Edge> edges, LineString geometry, State state
  ) {
    this.stop = stop;
    this.distance = distance;
    this.distanceIndependentTime = distanceIndependentTime;
    this.edges = edges;
    this.geometry = geometry;
    this.state = state;
  }

  public NearbyStop(
      TransitStopVertex stopVertex, double distance, List<Edge> edges, LineString geometry,
      State state
  ) {
    this(stopVertex.getStop(), distance, 0, edges, geometry, state);
  }

  @Override
  public int compareTo(NearbyStop that) {
    return (int) (this.distance) - (int) (that.distance);
  }

  public String toString() {
    return String.format("stop %s at %.1f meters", stop, distance);
  }

  /**
   * Given a State at a StopVertex, bundle the StopVertex together with information about how far
   * away it is and the geometry of the path leading up to the given State.
   */
  public static NearbyStop nearbyStopForState(State state, StopLocation stop) {
    double effectiveWalkDistance = 0.0;
    int distanceIndependentTime = 0;
    GraphPath graphPath = new GraphPath(state, false);
    CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();
    List<Edge> edges = new ArrayList<>();
    for (Edge edge : graphPath.edges) {
      LineString geometry = edge.getGeometry();
      if (geometry != null) {
        if (coordinates.size() == 0) {
          coordinates.extend(geometry.getCoordinates());
        }
        else {
          coordinates.extend(geometry.getCoordinates(), 1);
        }
      }
      effectiveWalkDistance += edge.getEffectiveWalkDistance();
      distanceIndependentTime += edge.getDistanceIndependentTime();
      edges.add(edge);
    }
    if (coordinates.size() < 2) {   // Otherwise the walk step generator breaks.
      ArrayList<Coordinate> coordinateList = new ArrayList<>(2);
      State lastState = graphPath.states.getLast();
      coordinateList.add(lastState.getVertex().getCoordinate());
      State backState = lastState.getBackState();
      coordinateList.add(Objects.requireNonNullElse(backState, lastState).getVertex().getCoordinate());
      coordinates = new CoordinateArrayListSequence(coordinateList);
    }
    return new NearbyStop(
        stop,
        effectiveWalkDistance,
        distanceIndependentTime,
        edges,
        geometryFactory.createLineString(new PackedCoordinateSequence.Double(coordinates.toCoordinateArray())),
        state
    );
  }
}
