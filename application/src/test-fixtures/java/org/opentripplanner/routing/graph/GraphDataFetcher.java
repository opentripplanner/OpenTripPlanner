package org.opentripplanner.routing.graph;

import java.text.DecimalFormat;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.GeoJsonIo;

public class GraphDataFetcher {

  public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.######");
  private final Graph graph;

  public GraphDataFetcher(Graph graph) {
    this.graph = graph;
  }

  public List<StreetEdge> listStreetEdges() {
    return graph.getEdgesOfType(StreetEdge.class);
  }

  public List<String> summarizeLinks() {
    return graph
      .getEdgesOfType(TemporaryPartialStreetEdge.class)
      .stream()
      .map(e ->
        String.format(
          "%s → %s %s ♿%s",
          summarizeVertex(e.getFromVertex()),
          summarizeVertex(e.getToVertex()),
          e.getPermission(),
          summarizeBoolean(e.isWheelchairAccessible())
        )
      )
      .toList();
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

  public List<TransitStopVertex> listStopVertices() {
    return graph.getVerticesOfType(TransitStopVertex.class);
  }

  public List<Edge> listEdges() {
    return List.copyOf(graph.getEdges());
  }

  public List<AreaEdge> listAreaEdges() {
    return graph.getEdgesOfType(AreaEdge.class);
  }

  public Graph graph() {
    return graph;
  }

  public String geoJsonUrl() {
    return GeoJsonIo.toUrl(graph);
  }

  private static String summarizeBoolean(boolean b) {
    if (b) {
      return "✅";
    } else {
      return "❌";
    }
  }

  private static String summarizeVertex(Vertex e) {
    return String.format(
      "(%s,%s)".formatted(DECIMAL_FORMAT.format(e.getLat()), DECIMAL_FORMAT.format(e.getLon()))
    );
  }
}
