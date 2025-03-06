package org.opentripplanner.routing.linking;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
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
    return Stream.concat(loc.getIncoming().stream(), loc.getOutgoing().stream())
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
    validateDistinctVertices(from, to);
    streetEdge.createPartialEdge(from, to).ifPresent(tempEdges::addEdge);
  }

  /**
   * If the origin and destination are very close to each other (within meters), they can be
   * projected on the same street edge at the same geographical location, in which case there is no
   * point in further processing the request. This method raises a
   * {@link RoutingErrorCode#WALKING_BETTER_THAN_TRANSIT} in this case.
   *
   * @throws RoutingValidationException if the from and to vertices are at the same geographical
   *                                    location.
   */
  private static void validateDistinctVertices(StreetVertex from, StreetVertex to) {
    if (from.sameLocation(to)) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, null))
      );
    }
  }
}
