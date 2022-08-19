package org.opentripplanner.routing.core;

import java.util.Collections;
import java.util.Set;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.refactor.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.refactor.request.NewRouteRequest;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.OTPFeature;

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

  public final NewRouteRequest opt;

  public final RoutingPreferences pref;

  public final Graph graph;

  public final Set<Vertex> fromVertices;

  public final Set<Vertex> toVertices;

  /**
   * DataOverlay Sandbox module context.
   */
  public DataOverlayContext dataOverlayContext;

  /* CONSTRUCTORS */

  /**
   * Constructor that automatically computes origin/target from TemporaryVerticesContainer.
   */
  public RoutingContext(
    NewRouteRequest request,
    RoutingPreferences preferences,
    Graph graph,
    TemporaryVerticesContainer temporaryVertices
  ) {
    this(
      request,
      preferences,
      graph,
      temporaryVertices.getFromVertices(),
      temporaryVertices.getToVertices()
    );
  }

  /**
   * Constructor that takes to/from vertices as input.
   */
  public RoutingContext(NewRouteRequest request, RoutingPreferences preferences, Graph graph, Vertex from, Vertex to) {
    this(request, preferences, graph, Collections.singleton(from), Collections.singleton(to));
  }

  /**
   * Constructor that takes sets of to/from vertices as input.
   */
  public RoutingContext(
    NewRouteRequest request,
    RoutingPreferences preferences,
    Graph graph,
    Set<Vertex> from,
    Set<Vertex> to
  ) {
    if (graph == null) {
      throw new GraphNotFoundException();
    }
    this.opt = request;
    this.pref = preferences;
    this.graph = graph;
    this.fromVertices = request.arriveBy() ? to : from;
    this.toVertices = request.arriveBy() ? from : to;
    this.dataOverlayContext =
      OTPFeature.DataOverlay.isOnElseNull(() ->
        new DataOverlayContext(graph.dataOverlayParameterBindings, preferences.system().dataOverlay())
      );
  }
}
