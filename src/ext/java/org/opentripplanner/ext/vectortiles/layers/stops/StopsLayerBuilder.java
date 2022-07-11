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
import org.opentripplanner.transit.service.TransitModel;

public class StopsLayerBuilder extends LayerBuilder<TransitStopVertex> {

  static Map<MapperType, Function<TransitModel, PropertyMapper<TransitStopVertex>>> mappers = Map.of(
    MapperType.Digitransit,
    DigitransitStopPropertyMapper::create
  );
  private final TransitModel transitModel;

  public StopsLayerBuilder(
    Graph graph,
    TransitModel transitModel,
    VectorTilesResource.LayerParameters layerParameters
  ) {
    super(
      layerParameters.name(),
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(transitModel)
    );
    this.transitModel = transitModel;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return transitModel
      .getStopSpatialIndex()
      .query(query)
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
