package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import static java.util.Map.entry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.util.geometry.GeometryUtils;

public class VehicleParkingsLayerBuilder extends LayerBuilder<VehicleParking> {

  static Map<VehicleParkingsLayerBuilder.MapperType, Function<Locale, PropertyMapper<VehicleParking>>> mappers = Map.ofEntries(
    entry(
      VehicleParkingsLayerBuilder.MapperType.Stadtnavi,
      StadtnaviVehicleParkingPropertyMapper::create
    ),
    entry(MapperType.Digitransit, DigitransitVehicleParkingPropertyMapper::create)
  );
  private final Graph graph;

  public VehicleParkingsLayerBuilder(
    Graph graph,
    VectorTilesResource.LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      layerParameters.name(),
      mappers
        .get(VehicleParkingsLayerBuilder.MapperType.valueOf(layerParameters.mapper()))
        .apply(locale)
    );
    this.graph = graph;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    VehicleParkingService service = graph.getVehicleParkingService();
    if (service == null) {
      return List.of();
    }
    return service
      .getVehicleParkings()
      .map(vehicleParking -> {
        Coordinate coordinate = vehicleParking.getCoordinate().asJtsCoordinate();
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(vehicleParking);
        return point;
      })
      .collect(Collectors.toList());
  }

  enum MapperType {
    Digitransit,
    Stadtnavi,
  }
}
