package org.opentripplanner.routing.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.graph_builder.linking.SameEdgeAdjuster;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

/**
 * This class is responsible for linking the RoutingRequest origin and destination to the Graph used
 * in the A-Star search, as well as removing them after the search has been done. It implements
 * AutoCloseable, in order to be able to use the try-with-resources statement, making the clean-up
 * automatic.
 */
public class TemporaryVerticesContainer implements AutoCloseable {

  private final Graph graph;
  private final RoutingRequest opt;
  private final Set<DisposableEdgeCollection> tempEdges;
  private final Set<Vertex> fromVertices;
  private final Set<Vertex> toVertices;

  public TemporaryVerticesContainer(Graph graph, RoutingRequest opt) {
    this.tempEdges = new HashSet<>();

    this.graph = graph;
    StreetVertexIndex index = this.graph.getStreetIndex();
    this.opt = opt;
    fromVertices = index.getVerticesForLocation(opt.from, opt, false, tempEdges);
    toVertices = index.getVerticesForLocation(opt.to, opt, true, tempEdges);

    checkIfVerticesFound(opt.arriveBy);

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
   * Tear down this container, removing any temporary edges from
   * the "permanent" graph objects. This enables all temporary objects
   * for garbage collection.
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

  /* PRIVATE METHODS */

  private void checkIfVerticesFound(boolean arriveBy) {
    List<RoutingError> routingErrors = new ArrayList<>();

    var from = arriveBy ? toVertices : fromVertices;
    var to = arriveBy ? fromVertices : toVertices;

    // check that vertices where found if from-location was specified
    if (opt.from.isSpecified() && isDisconnected(from, true)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(opt.from), InputField.FROM_PLACE)
      );
    }

    // check that vertices where found if to-location was specified
    if (opt.to.isSpecified() && isDisconnected(to, false)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(opt.to), InputField.TO_PLACE)
      );
    }

    if (routingErrors.size() > 0) {
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
    Predicate<Vertex> isNotConnected = (isFrom == opt.arriveBy) ? hasNoIncoming : hasNoOutgoing;

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
