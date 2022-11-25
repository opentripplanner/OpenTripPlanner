package org.opentripplanner.routing.linking;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class SameEdgeAdjuster {

  private SameEdgeAdjuster() {}

  /**
   * Utility class. If the from and to vertices are generated and lie along some of the same edges,
   * we need to wire them up along those edges so that we don't get odd circuitous routes for really
   * short trips.
   */
  public static DisposableEdgeCollection adjust(Vertex from, Vertex to, Graph graph) {
    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph, Scope.REQUEST);
    if (from == null || to == null) {
      return tempEdges;
    }

    try {
      Set<StreetVertex> fromVertices = new HashSet<>();

      for (Edge outgoing : from.getOutgoing()) {
        Vertex toVertex = outgoing.getToVertex();
        if (
          outgoing instanceof TemporaryFreeEdge &&
          toVertex instanceof StreetVertex &&
          toVertex
            .getOutgoing()
            .stream()
            .anyMatch(edge -> edge instanceof TemporaryPartialStreetEdge)
        ) {
          // The vertex is connected with an TemporaryFreeEdge connector to the
          // TemporaryPartialStreetEdge
          fromVertices.add((StreetVertex) toVertex);
        } else if (outgoing instanceof TemporaryPartialStreetEdge && from instanceof StreetVertex) {
          fromVertices.add((StreetVertex) from);
        }
      }

      Set<StreetVertex> toVertices = new HashSet<>();

      for (Edge incoming : to.getIncoming()) {
        Vertex fromVertex = incoming.getFromVertex();
        if (
          incoming instanceof TemporaryFreeEdge &&
          fromVertex instanceof StreetVertex &&
          fromVertex
            .getIncoming()
            .stream()
            .anyMatch(edge -> edge instanceof TemporaryPartialStreetEdge)
        ) {
          // The vertex is connected with an TemporaryFreeEdge connector to the
          // TemporaryPartialStreetEdge
          toVertices.add((StreetVertex) fromVertex);
        } else if (incoming instanceof TemporaryPartialStreetEdge && to instanceof StreetVertex) {
          toVertices.add((StreetVertex) to);
        }
      }

      for (StreetVertex fromStreetVertex : fromVertices) {
        for (StreetVertex toStreetVertex : toVertices) {
          Set<StreetEdge> overlap = overlappingParentStreetEdges(fromStreetVertex, toStreetVertex);
          for (StreetEdge pse : overlap) {
            makePartialEdgeAlong(pse, fromStreetVertex, toStreetVertex, tempEdges);
          }
        }
      }
    } catch (Exception e) {
      tempEdges.disposeEdges();
      throw e;
    }

    return tempEdges;
  }

  /**
   * Returns the StreetEdges that overlap between two vertices' edge sets. It does not look at the
   * TemporaryPartialStreetEdges, but the real parents of these edges.
   */
  private static Set<StreetEdge> overlappingParentStreetEdges(Vertex u, Vertex v) {
    // Fetch the parent edges so we aren't stuck with temporary edges.
    Set<StreetEdge> vEdges = getConnectedParentEdges(v);
    Set<StreetEdge> uEdges = getConnectedParentEdges(u);
    return Sets.intersection(uEdges, vEdges);
  }

  /**
   * Find all parent edges ({@link TemporaryPartialStreetEdge#getParentEdge()}) for {@link
   * Vertex#getIncoming()} and {@link Vertex#getIncoming()} edges. Edges of other types are
   * ignored.
   */
  private static Set<StreetEdge> getConnectedParentEdges(Vertex loc) {
    return Stream
      .concat(loc.getIncoming().stream(), loc.getOutgoing().stream())
      .filter(it -> it instanceof TemporaryPartialStreetEdge)
      .map(it -> ((TemporaryPartialStreetEdge) it).getParentEdge())
      .collect(Collectors.toSet());
  }

  /**
   * Creates a PartialStreetEdge along the input StreetEdge iff its direction makes this possible.
   */
  private static void makePartialEdgeAlong(
    StreetEdge streetEdge,
    StreetVertex from,
    StreetVertex to,
    DisposableEdgeCollection tempEdges
  ) {
    streetEdge.createPartialEdge(from, to).ifPresent(tempEdges::addEdge);
  }
}
