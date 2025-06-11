package org.opentripplanner.inspector.vector.geofencing;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclerental.street.NoRestriction;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * A vector tile layer containing all {@link AreaStop}s inside the vector tile bounds.
 */
public class GeofencingZonesLayerBuilder extends LayerBuilder<Vertex> {

  private final Graph graph;

  public GeofencingZonesLayerBuilder(Graph graph, LayerParameters layerParameters) {
    super(
      new GeofencingZonesPropertyMapper(),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return graph
      .findVertices(query)
      .stream()
      .filter(se -> !(se.rentalRestrictions() instanceof NoRestriction))
      .map(vertex -> {
        Geometry geometry = GeometryUtils.getGeometryFactory().createPoint(vertex.getCoordinate());
        geometry.setUserData(vertex);
        return geometry;
      })
      .toList();
  }
}
