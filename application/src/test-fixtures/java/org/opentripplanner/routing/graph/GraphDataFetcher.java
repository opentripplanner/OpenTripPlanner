package org.opentripplanner.routing.graph;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.TemporaryPartialStreetEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.GeoJsonIo;

public class GraphDataFetcher {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
    "0.######",
    new DecimalFormatSymbols(Locale.ROOT)
  );
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

  public Collection<String> summarizeSplitVertices() {
    return graph
      .getVerticesOfType(SplitterVertex.class)
      .stream()
      .map(GraphDataFetcher::summarizeVertex)
      .toList();
  }

  /**
   * A list of textual representations of the links (edges of type {@link TemporaryPartialStreetEdge})
   * in the graph.
   */
  public Collection<String> summarizeLinks() {
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

  private static String summarizeBoolean(boolean b) {
    if (b) {
      return "✅";
    } else {
      return "❌";
    }
  }

  private static String summarizeVertex(Vertex v) {
    var buf = new StringBuilder();

    buf.append(
      String.format(
        "(%s,%s)".formatted(DECIMAL_FORMAT.format(v.getLat()), DECIMAL_FORMAT.format(v.getLon()))
      )
    );

    if (!v.areaStops().isEmpty()) {
      buf
        .append("[areaStops=")
        .append(
          String.join(",", v.areaStops().stream().map(FeedScopedId::toString).sorted().toList())
        )
        .append("]");
    }

    return buf.toString();
  }
}
