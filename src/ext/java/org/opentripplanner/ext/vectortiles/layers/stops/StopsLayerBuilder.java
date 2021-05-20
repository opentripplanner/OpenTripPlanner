package org.opentripplanner.ext.vectortiles.layers.stops;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StopsLayerBuilder extends LayerBuilder<TransitStopVertex> {
  enum MapperType { Digitransit }

  static Map<MapperType, Function<Graph, PropertyMapper<TransitStopVertex>>> mappers = Map.of(
      MapperType.Digitransit, DigitransitStopPropertyMapper::create
  );

  private final Graph graph;

  public StopsLayerBuilder(Graph graph, VectorTilesResource.LayerParameters layerParameters) {
    super(
        layerParameters.name(),
        mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(graph)
    );

    this.graph = graph;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return graph.index.getStopSpatialIndex().query(query).stream().map(transitStopVertex -> {
      Point point = GeometryUtils
          .getGeometryFactory()
          .createPoint(transitStopVertex.getCoordinate());

      point.setUserData(transitStopVertex);

      return point;
    }).collect(Collectors.toList());
  }
}
