package org.opentripplanner.inspector.vector.geofencing;

import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.service.vehiclerental.street.NoRestriction;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * A vector tile layer containing all {@link AreaStop}s inside the vector tile bounds.
 */
public class GeofencingZonesLayerBuilder extends LayerBuilder<Vertex> {

  private static final Map<MapperType, MapperFactory> mappers = Map.of(
    MapperType.DebugClient,
    transitService -> new GeofencingZonesPropertyMapper()
  );
  private final StreetIndex streetIndex;

  public GeofencingZonesLayerBuilder(Graph graph, LayerParameters layerParameters) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())).build(graph),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.streetIndex = graph.getStreetIndex();
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return streetIndex
      .getVerticesForEnvelope(query)
      .stream()
      .filter(se -> !(se.rentalRestrictions() instanceof NoRestriction))
      .map(vertex -> {
        Geometry geometry = GeometryUtils.getGeometryFactory().createPoint(vertex.getCoordinate());
        geometry.setUserData(vertex);
        return geometry;
      })
      .toList();
  }

  enum MapperType {
    DebugClient,
  }

  @FunctionalInterface
  private interface MapperFactory {
    PropertyMapper<Vertex> build(Graph transitService);
  }
}
