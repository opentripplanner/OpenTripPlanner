package org.opentripplanner.ext.vectortiles.layers.bikerental;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.graph.Graph;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BikeRentalLayerBuilder extends LayerBuilder<BikeRentalStation> {
  enum MapperType { Digitransit }

  static Map<MapperType, Function<Graph, PropertyMapper<BikeRentalStation>>> mappers = Map.of(
      MapperType.Digitransit, DigitransitBikeRentalPropertyMapper::create
  );

  private final Graph graph;

  public BikeRentalLayerBuilder(Graph graph, VectorTilesResource.LayerParameters layerParameters) {
    super(
        layerParameters.name(),
        mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(graph)
    );

    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    BikeRentalStationService service = graph.getBikerentalStationService();
    if (service == null) {return List.of();}
    return service.getBikeRentalStations()
        .stream()
        .map(bikeRentalStation -> {
          Coordinate coordinate = new Coordinate(bikeRentalStation.x, bikeRentalStation.y);
          Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
          point.setUserData(bikeRentalStation);
          return point;
        })
        .collect(Collectors.toList());
  }
}
