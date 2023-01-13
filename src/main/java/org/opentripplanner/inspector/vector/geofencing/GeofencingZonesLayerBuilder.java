package org.opentripplanner.inspector.vector.geofencing;

import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.api.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * A vector tile layer containing all {@link AreaStop}s inside the vector tile bounds.
 */
public class GeofencingZonesLayerBuilder extends LayerBuilder<StreetEdge> {

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
      .getEdgesForEnvelope(query)
      .stream()
      .filter(StreetEdge.class::isInstance)
      .map(StreetEdge.class::cast)
      .filter(se -> !(se.getTraversalExtension() instanceof StreetEdgeRentalExtension.NoExtension))
      .map(edge -> {
        Geometry geometry = edge.getGeometry().copy();
        geometry.setUserData(edge);
        return geometry;
      })
      .toList();
  }

  enum MapperType {
    DebugClient,
  }

  @FunctionalInterface
  private interface MapperFactory {
    PropertyMapper<StreetEdge> build(Graph transitService);
  }
}
