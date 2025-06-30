package org.opentripplanner.street.search;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.FromToViaVertexRequest;

/**
 * This class contains temporary vertices and edges that are used in A-Star searches. After they
 * are, no longer needed, this class removes the temporary vertices and edges. It implements
 * AutoCloseable, in order to be able to use the try-with-resources statement, making the clean-up
 * automatic.
 */
public class TemporaryVerticesContainer implements AutoCloseable {

  private final List<DisposableEdgeCollection> tempEdges;
  private final Set<Vertex> fromVertices;
  private final Set<Vertex> toVertices;
  private final Map<VisitViaLocation, Set<Vertex>> visitViaLocationVertices;
  private final Set<TransitStopVertex> fromStopVertices;
  private final Set<TransitStopVertex> toStopVertices;

  public TemporaryVerticesContainer(TemporaryVerticesContainerBuilder builder) {
    this.tempEdges = builder.tempEdges();
    this.fromVertices = builder.fromVertices();
    this.fromStopVertices = builder.fromStopVertices();
    this.toVertices = builder.toVertices();
    this.visitViaLocationVertices = builder.visitViaLocationVertices();
    this.toStopVertices = builder.toStopVertices();
  }

  public static TemporaryVerticesContainerBuilder of(Graph graph) {
    return new TemporaryVerticesContainerBuilder(graph);
  }

  public Set<Vertex> fromVertices() {
    return fromVertices;
  }

  public Set<Vertex> toVertices() {
    return toVertices;
  }

  Map<VisitViaLocation, Set<Vertex>> visitViaLocationVertices() {
    return visitViaLocationVertices;
  }

  Set<TransitStopVertex> fromStopVertices() {
    return fromStopVertices;
  }

  Set<TransitStopVertex> toStopVertices() {
    return toStopVertices;
  }

  /**
   * Creates a {@link FromToViaVertexRequest} that contains vertices from this container.
   */
  public FromToViaVertexRequest createFromToViaVertexRequest() {
    return new FromToViaVertexRequest(
      fromVertices,
      toVertices,
      fromStopVertices,
      toStopVertices,
      visitViaLocationVertices
    );
  }

  /**
   * Tear down this container, removing any temporary edges from the "permanent" graph objects. This
   * enables all temporary objects for garbage collection.
   */
  @Override
  public void close() {
    this.tempEdges.forEach(DisposableEdgeCollection::disposeEdges);
  }
}
