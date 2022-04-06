package org.opentripplanner.routing.core;

import java.util.Collections;
import java.util.Set;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RoutingContext holds information needed to carry out a search for a particular TraverseOptions,
 * on a specific graph, using specified endpoint vertices.
 * <p>
 * In addition, while the RoutingRequest should only carry parameters _in_ to the routing operation,
 * the routing context should be used to carry information back out, such as debug figures or flags
 * that certain thresholds have been exceeded.
 */
public class RoutingContext {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingContext.class);

  /* FINAL FIELDS */

  public final RoutingRequest opt;

  public final Graph graph;

  public final Set<Vertex> fromVertices;

  public final Set<Vertex> toVertices;

  /** Indicates that the search timed out or was otherwise aborted. */
  public boolean aborted;

  /**
   * Indicates that a maximum slope constraint was specified but was removed during routing to
   * produce a result.
   */
  public boolean slopeRestrictionRemoved = false;

  /**
   * DataOverlay Sandbox module context.
   */
  public DataOverlayContext dataOverlayContext;

  /* CONSTRUCTORS */

  /**
   * Constructor that automatically computes origin/target from TemporaryVerticesContainer.
   */
  public RoutingContext(
    RoutingRequest routingRequest,
    Graph graph,
    TemporaryVerticesContainer temporaryVertices
  ) {
    this(
      routingRequest,
      graph,
      temporaryVertices.getFromVertices(),
      temporaryVertices.getToVertices()
    );
  }

  /**
   * Constructor that takes to/from vertices as input.
   */
  public RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to) {
    this(routingRequest, graph, Collections.singleton(from), Collections.singleton(to));
  }

  /**
   * Constructor that takes sets of to/from vertices as input.
   */
  public RoutingContext(
    RoutingRequest routingRequest,
    Graph graph,
    Set<Vertex> from,
    Set<Vertex> to
  ) {
    if (graph == null) {
      throw new GraphNotFoundException();
    }
    this.opt = routingRequest;
    this.graph = graph;
    this.fromVertices = routingRequest.arriveBy ? to : from;
    this.toVertices = routingRequest.arriveBy ? from : to;
    this.dataOverlayContext =
      OTPFeature.DataOverlay.isOnElseNull(() ->
        new DataOverlayContext(graph.dataOverlayParameterBindings, routingRequest.dataOverlay)
      );
  }
}
