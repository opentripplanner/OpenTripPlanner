package org.opentripplanner.ext.carpooling.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * The portion of the street graph a search may traverse: the permanent graph, optionally plus the
 * temporary edges of one linking.
 * <p>
 * Request-scoped linking attaches temporary edges to permanent vertices, so a search over the shared
 * graph can reach another request's temporary subgraph. Some of those edges are mode-blind — a
 * {@code TemporaryFreeEdge} joining a location to its splitters carries no traversal permission — so
 * crossing one can make a vertex look car-reachable when no car could drive there. A scope pins down
 * which temporary edges a search is allowed to use, keeping foreign linkings out.
 * <p>
 * {@link #STATIC_GRAPH} excludes every temporary edge and describes a graph that outlives any single
 * request. {@link #withOwnLinkingOf} additionally admits the temporary edges of the given vertex's
 * own linking, which a search departing from a temporary vertex needs in order to reach the permanent
 * graph at all.
 */
final class TraversalScope implements SkipEdgeStrategy<State, Edge> {

  /** The permanent graph alone; every temporary edge is excluded. */
  static final TraversalScope STATIC_GRAPH = new TraversalScope(Set.of());

  private final Set<Vertex> ownTemporaryVertices;

  private TraversalScope(Set<Vertex> ownTemporaryVertices) {
    this.ownTemporaryVertices = ownTemporaryVertices;
  }

  /**
   * The permanent graph plus {@code start}'s own linking — every temporary vertex reachable from
   * {@code start} without crossing a permanent one. Foreign linkings attach only to the permanent
   * graph, so this never spreads into another request's subgraph. Equal to {@link #STATIC_GRAPH}
   * when {@code start} is permanent.
   */
  static TraversalScope withOwnLinkingOf(Vertex start) {
    var ownTemporaryVertices = ownLinking(start);
    return ownTemporaryVertices.isEmpty() ? STATIC_GRAPH : new TraversalScope(ownTemporaryVertices);
  }

  /** Whether {@code vertex} is part of the permanent graph, and so outlives the current request. */
  static boolean isPermanent(Vertex vertex) {
    return !(vertex instanceof TemporaryVertex);
  }

  /** Skips the temporary edges outside this scope: those with neither endpoint in its own linking. */
  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    return (
      edge instanceof TemporaryEdge &&
      !ownTemporaryVertices.contains(edge.getFromVertex()) &&
      !ownTemporaryVertices.contains(edge.getToVertex())
    );
  }

  /**
   * The permanent vertices bordering this scope's linking (split-edge endpoints or directly-linked
   * graph vertices), ordered by distance from {@code origin}. Empty for {@link #STATIC_GRAPH}.
   */
  List<Vertex> permanentBoundary(Vertex origin) {
    var seen = new HashSet<>(ownTemporaryVertices);
    var boundary = new ArrayList<Vertex>();
    for (var temporaryVertex : ownTemporaryVertices) {
      forEachNeighbor(temporaryVertex, neighbor -> {
        if (seen.add(neighbor)) {
          boundary.add(neighbor);
        }
      });
    }
    Coordinate originCoordinate = origin.getCoordinate();
    boundary.sort(
      Comparator.comparingDouble(v ->
        SphericalDistanceLibrary.fastDistance(originCoordinate, v.getCoordinate())
      )
    );
    return boundary;
  }

  /**
   * Every temporary vertex reachable from {@code start} without crossing a permanent one. Empty when
   * {@code start} is permanent.
   */
  private static Set<Vertex> ownLinking(Vertex start) {
    if (isPermanent(start)) {
      return Set.of();
    }
    var own = new HashSet<Vertex>();
    var queue = new ArrayDeque<Vertex>();
    own.add(start);
    queue.add(start);
    while (!queue.isEmpty()) {
      forEachNeighbor(queue.poll(), neighbor -> {
        if (!isPermanent(neighbor) && own.add(neighbor)) {
          queue.add(neighbor);
        }
      });
    }
    return own;
  }

  /** Applies {@code action} to both endpoints of every edge touching {@code vertex}. */
  private static void forEachNeighbor(Vertex vertex, Consumer<Vertex> action) {
    for (var edges : List.of(vertex.getOutgoing(), vertex.getIncoming())) {
      for (Edge edge : edges) {
        action.accept(edge.getFromVertex());
        action.accept(edge.getToVertex());
      }
    }
  }
}
