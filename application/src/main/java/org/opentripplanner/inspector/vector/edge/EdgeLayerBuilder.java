package org.opentripplanner.inspector.vector.edge;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Selects all edges to be displayed for debugging.
 */
public class EdgeLayerBuilder extends LayerBuilder<Edge> {

  private final Graph graph;

  public EdgeLayerBuilder(Graph graph, LayerParameters layerParameters) {
    super(new EdgePropertyMapper(), layerParameters.name(), layerParameters.expansionFactor());
    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return graph
      .findEdges(query)
      .stream()
      .filter(e -> e.getGeometry() != null)
      .map(edge -> {
        Geometry geometry = edge.getGeometry();
        geometry.setUserData(edge);
        return geometry;
      })
      .toList();
  }
}
