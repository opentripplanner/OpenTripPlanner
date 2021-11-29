package org.opentripplanner.ext.vectortiles.layers.stations;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.model.Station;
import org.opentripplanner.routing.graph.Graph;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StationsLayerBuilder extends LayerBuilder<Station> {
  enum MapperType { Digitransit }

  static Map<MapperType, Function<Graph, PropertyMapper<Station>>> mappers = Map.of(
      MapperType.Digitransit, DigitransitStationPropertyMapper::create
  );

  private final Graph graph;

  public StationsLayerBuilder(Graph graph, VectorTilesResource.LayerParameters layerParameters) {
    super(
        layerParameters.name(),
        mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(graph)
    );
    this.graph = graph;
  }

  protected List<Geometry> getGeometries(Envelope query) {
    return graph.getStations().stream().map(station -> {
      Coordinate coordinate = station.getCoordinate().asJtsCoordinate();
      Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
      point.setUserData(station);
      return point;
    }).collect(Collectors.toList());
  }
}
