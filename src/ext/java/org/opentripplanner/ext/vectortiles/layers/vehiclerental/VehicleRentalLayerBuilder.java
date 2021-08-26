package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.graph.Graph;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VehicleRentalLayerBuilder extends LayerBuilder<VehicleRentalStation> {
  enum MapperType { Digitransit }

  static Map<MapperType, Function<Graph, PropertyMapper<VehicleRentalStation>>> mappers = Map.of(
      MapperType.Digitransit, DigitransitBikeRentalPropertyMapper::create
  );

  private final Graph graph;

  public VehicleRentalLayerBuilder(Graph graph, VectorTilesResource.LayerParameters layerParameters) {
    super(
        layerParameters.name(),
        mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(graph)
    );

    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    VehicleRentalStationService service = graph.getVehicleRentalStationService();
    if (service == null) {return List.of();}
    return service.getVehicleRentalStations()
        .stream()
        .map(bikeRentalStation -> {
          Coordinate coordinate = new Coordinate(bikeRentalStation.longitude, bikeRentalStation.latitude);
          Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
          point.setUserData(bikeRentalStation);
          return point;
        })
        .collect(Collectors.toList());
  }
}
