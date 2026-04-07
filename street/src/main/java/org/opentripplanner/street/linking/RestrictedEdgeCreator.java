package org.opentripplanner.street.linking;

import javax.annotation.Nullable;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Functional interface that is used to create edges between vertices during the linking process.
 * This interface's method contains additional permission parameter that {@link EdgeCreator} doesn't
 * have. The resulting edge from this interface can restrict traversal based on the given
 * permission, but doesn't necessarily need to.
 * <p>
 * This interface can be used in both directions, with different types of vertices and with
 * different types of links between the vertices. Therefore, the parameters use the generic
 * {@link Vertex} and the return type uses the {@link Edge} class.
 */
@FunctionalInterface
public interface RestrictedEdgeCreator {
  /**
   * Creates an edge from the fromVertex to the toVertex. The resulting edge might restrict
   * traversal based on the given permission.
   *
   * @return The created edge or {@code null} if no edge is created.
   */
  @Nullable
  Edge create(Vertex fromVertex, Vertex toVertex, StreetTraversalPermission permission);
}
