package org.opentripplanner.street.search;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.SameEdgeAdjuster;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * This class is responsible for linking the RouteRequest origin and destination to the Graph used
 * in the A-Star search, as well as removing them after the search has been done. It implements
 * AutoCloseable, in order to be able to use the try-with-resources statement, making the clean-up
 * automatic.
 */
public class TemporaryVerticesContainer implements AutoCloseable {

  private final Graph graph;
  private final Set<DisposableEdgeCollection> tempEdges;
  private final Set<Vertex> fromVertices;
  private final Set<Vertex> toVertices;
  private final GenericLocation from;
  private final GenericLocation to;

  public TemporaryVerticesContainer(
    Graph graph,
    GenericLocation from,
    GenericLocation to,
    StreetMode accessMode,
    StreetMode egressMode
  ) {
    this.tempEdges = new HashSet<>();

    this.graph = graph;
    StreetIndex index = this.graph.getStreetIndex();
    this.from = from;
    this.to = to;
    fromVertices = index.getStreetVerticesForLocation(from, accessMode, false, tempEdges);
    toVertices = index.getStreetVerticesForLocation(to, egressMode, true, tempEdges);

    checkIfVerticesFound();

    if (fromVertices != null && toVertices != null) {
      for (Vertex fromVertex : fromVertices) {
        for (Vertex toVertex : toVertices) {
          tempEdges.add(SameEdgeAdjuster.adjust(fromVertex, toVertex, graph));
        }
      }
    }
  }

  /* INSTANCE METHODS */

  /**
   * Tear down this container, removing any temporary edges from the "permanent" graph objects. This
   * enables all temporary objects for garbage collection.
   */
  public void close() {
    this.tempEdges.forEach(DisposableEdgeCollection::disposeEdges);
  }

  public Set<Vertex> getFromVertices() {
    return fromVertices;
  }

  public Set<Vertex> getToVertices() {
    return toVertices;
  }

  /**
   * Get the stop vertices that corresponds to the from location. If the from location only contains
   * coordinates, this will return an empty set. If the from location is a station id this will
   * return the child stops of that station.
   */
  public Set<TransitStopVertex> getFromStopVertices() {
    StreetIndex index = this.graph.getStreetIndex();
    if (from.stopId == null) {
      return Set.of();
    }
    return index.getStopOrChildStopsVertices(from.stopId);
  }

  /**
   * Get the stop vertices that corresponds to the to location. If the to location only contains
   * coordinates, this will return an empty set. If the to location is a station id this will
   * return the child stops of that station.
   */
  public Set<TransitStopVertex> getToStopVertices() {
    StreetIndex index = this.graph.getStreetIndex();
    if (to.stopId == null) {
      return Set.of();
    }
    return index.getStopOrChildStopsVertices(to.stopId);
  }

  /* PRIVATE METHODS */

  private void checkIfVerticesFound() {
    List<RoutingError> routingErrors = new ArrayList<>();

    // check that vertices where found if from-location was specified
    if (from.isSpecified() && isDisconnected(fromVertices, true)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(from), InputField.FROM_PLACE)
      );
    }

    // check that vertices where found if to-location was specified
    if (to.isSpecified() && isDisconnected(toVertices, false)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(to), InputField.TO_PLACE)
      );
    }

    // if from and to share any vertices, the user is already at their destination, and the result
    // is a trivial path
    if (
      fromVertices != null &&
      toVertices != null &&
      !Sets.intersection(fromVertices, toVertices).isEmpty()
    ) {
      routingErrors.add(new RoutingError(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, null));
    }

    if (!routingErrors.isEmpty()) {
      throw new RoutingValidationException(routingErrors);
    }
  }

  private boolean isDisconnected(Set<Vertex> vertices, boolean isFrom) {
    // Not connected if linking was not attempted, and vertices were not specified in the request.
    if (vertices == null) {
      return true;
    }

    Predicate<Vertex> isNotTransit = Predicate.not(TransitStopVertex.class::isInstance);
    Predicate<Vertex> hasNoIncoming = v -> v.getIncoming().isEmpty();
    Predicate<Vertex> hasNoOutgoing = v -> v.getOutgoing().isEmpty();

    // Not connected if linking did not create incoming/outgoing edges depending on the
    // direction and the end.
    Predicate<Vertex> isNotConnected = isFrom ? hasNoOutgoing : hasNoIncoming;

    return vertices.stream().allMatch(isNotTransit.and(isNotConnected));
  }

  private RoutingErrorCode getRoutingErrorCodeForDisconnected(GenericLocation location) {
    Coordinate coordinate = location.getCoordinate();
    GeometryFactory gf = GeometryUtils.getGeometryFactory();
    return coordinate != null && graph.getConvexHull().disjoint(gf.createPoint(coordinate))
      ? RoutingErrorCode.OUTSIDE_BOUNDS
      : RoutingErrorCode.LOCATION_NOT_FOUND;
  }
}
