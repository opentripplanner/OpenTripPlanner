package org.opentripplanner.routing.core;

import java.util.Collections;
import java.util.Set;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A RoutingContext holds information needed to carry out a search for a particular TraverseOptions,
 * on a specific graph, using specified endpoint vertices.
 * <p>
 * In addition, while the RoutingRequest should only carry parameters _in_ to the routing operation,
 * the routing context should be used to carry information back out, such as debug figures or flags
 * that certain thresholds have been exceeded.
 */
public class RoutingContext {

  /* FINAL FIELDS */

  public final RouteRequest opt;

  public final Set<Vertex> fromVertices;

  public final Set<Vertex> toVertices;

  /* CONSTRUCTORS */

  /**
   * Constructor that automatically computes origin/target from TemporaryVerticesContainer.
   */
  public RoutingContext(RouteRequest routingRequest, TemporaryVerticesContainer temporaryVertices) {
    this(routingRequest, temporaryVertices.getFromVertices(), temporaryVertices.getToVertices());
  }

  /**
   * Constructor that takes to/from vertices as input.
   */
  public RoutingContext(RouteRequest routingRequest, Vertex from, Vertex to) {
    this(routingRequest, Collections.singleton(from), Collections.singleton(to));
  }

  /**
   * Constructor that takes sets of to/from vertices as input.
   */
  public RoutingContext(RouteRequest routingRequest, Set<Vertex> from, Set<Vertex> to) {
    this.opt = routingRequest;
    this.fromVertices = routingRequest.arriveBy() ? to : from;
    this.toVertices = routingRequest.arriveBy() ? from : to;
  }
}
