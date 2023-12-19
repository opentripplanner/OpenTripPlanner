package org.opentripplanner.inspector.vector.edge;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * A vector tile layer containing all {@link AreaStop}s inside the vector tile bounds.
 */
public class EdgeLayerBuilder extends LayerBuilder<Edge> {

  private final StreetIndex streetIndex;

  public EdgeLayerBuilder(Graph graph, LayerParameters layerParameters) {
    super(new EdgePropertyMapper(), layerParameters.name(), layerParameters.expansionFactor());
    this.streetIndex = graph.getStreetIndex();
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return streetIndex
      .getEdgesForEnvelope(query)
      .stream()
      .filter(edge -> edge.getGeometry() != null)
      .map(edge -> {
        Geometry geometry = edge.getGeometry().copy();
        geometry.setUserData(edge);
        return geometry;
      })
      .toList();
  }
}
