package org.opentripplanner.routing.graph;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.GeoJsonIo;

public class GraphDataFetcher {

  private final Graph graph;

  public GraphDataFetcher(Graph graph) {
    this.graph = graph;
  }

  /**
   * Converts the input to a string-based label and looks it up in the graph. Remember that there
   * are other, non-string vertex labels for which this method will not work.
   * @see VertexLabel
   */
  @Nullable
  public Vertex getVertex(String label) {
    return graph.getVertex(VertexLabel.string(label));
  }

  public List<StreetEdge> listStreetEdges() {
    return graph.getEdgesOfType(StreetEdge.class);
  }

  public List<TransitStopVertex> listStopVertices() {
    return graph.getVerticesOfType(TransitStopVertex.class);
  }

  /**
   * Iterates over all vertices in the graph and gets all incoming _and_ outgoing edges.
   * This is a different behavior than {@link Graph#getEdges()}, which only returns edges that are
   * outgoing.
   */
  public List<Edge> listEdges() {
    return graph
      .getVertices()
      .stream()
      .flatMap(v -> Stream.concat(v.getOutgoing().stream(), v.getIncoming().stream()))
      .distinct()
      .toList();
  }

  public List<AreaEdge> listAreaEdges() {
    return graph.getEdgesOfType(AreaEdge.class);
  }

  public String geoJsonUrl() {
    return GeoJsonIo.toUrl(this.listEdges(), graph.getVertices());
  }

  public Collection<String> summarizeSplitVertices() {
    return graph
      .getVerticesOfType(SplitterVertex.class)
      .stream()
      .map(StreetSummarizer::summarizeVertex)
      .toList();
  }

  /**
   * A list of textual representations of the links (edges of type {@link TemporaryPartialStreetEdge})
   * in the graph.
   */
  public Collection<String> summarizeTempEdges() {
    return listEdges()
      .stream()
      .filter(e -> e instanceof TemporaryEdge)
      .map(StreetSummarizer::summarizeEdge)
      .toList();
  }

  public Graph graph() {
    return graph;
  }
}
