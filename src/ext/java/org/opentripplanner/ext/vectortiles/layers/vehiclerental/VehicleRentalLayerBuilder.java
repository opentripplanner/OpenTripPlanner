package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;

public class VehicleRentalLayerBuilder extends LayerBuilder<VehicleRentalPlace> {

  static Map<MapperType, Function<Graph, PropertyMapper<VehicleRentalPlace>>> mappers = Map.of(
    MapperType.Digitransit,
    g -> DigitransitVehicleRentalPropertyMapper.create()
  );
  private final Graph graph;

  public VehicleRentalLayerBuilder(
    Graph graph,
    VectorTilesResource.LayerParameters layerParameters
  ) {
    super(
      layerParameters.name(),
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(graph)
    );
    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    VehicleRentalStationService service = graph.getVehicleRentalStationService();
    if (service == null) {
      return List.of();
    }
    return service
      .getVehicleRentalPlaces()
      .stream()
      .map(vehicleRentalStation -> {
        Coordinate coordinate = new Coordinate(
          vehicleRentalStation.getLongitude(),
          vehicleRentalStation.getLatitude()
        );
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(vehicleRentalStation);
        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
  }
}
