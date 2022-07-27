package org.opentripplanner.ext.vectortiles.layers.stops;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.service.TransitService;

public class StopsLayerBuilder extends LayerBuilder<TransitStopVertex> {

  static Map<MapperType, Function<TransitService, PropertyMapper<TransitStopVertex>>> mappers = Map.of(
    MapperType.Digitransit,
    DigitransitStopPropertyMapper::create
  );
  private final TransitService transitService;

  public StopsLayerBuilder(
    Graph graph,
    TransitService transitService,
    VectorTilesResource.LayerParameters layerParameters
  ) {
    super(
      layerParameters.name(),
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitService)
    );
    this.transitService = transitService;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return transitService
      .queryStopSpatialIndex(query)
      .stream()
      .map(transitStopVertex -> {
        Point point = GeometryUtils
          .getGeometryFactory()
          .createPoint(transitStopVertex.getCoordinate());

        point.setUserData(transitStopVertex);

        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
  }
}
