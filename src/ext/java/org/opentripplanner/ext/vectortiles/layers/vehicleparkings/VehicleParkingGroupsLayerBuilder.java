package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.util.geometry.GeometryUtils;

public class VehicleParkingGroupsLayerBuilder extends LayerBuilder<VehicleParkingAndGroup> {

  static Map<VehicleParkingGroupsLayerBuilder.MapperType, Function<Locale, PropertyMapper<VehicleParkingAndGroup>>> mappers = Map.of(
    VehicleParkingGroupsLayerBuilder.MapperType.Digitransit,
    DigitransitVehicleParkingGroupPropertyMapper::create
  );
  private final Graph graph;

  public VehicleParkingGroupsLayerBuilder(
    Graph graph,
    VectorTilesResource.LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      layerParameters.name(),
      mappers
        .get(VehicleParkingGroupsLayerBuilder.MapperType.valueOf(layerParameters.mapper()))
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
      .getVehicleParkingGroups()
      .asMap()
      .entrySet()
      .stream()
      .map(vehicleParkingGroupEntry -> {
        var group = vehicleParkingGroupEntry.getKey();
        Coordinate coordinate = group.coordinate().asJtsCoordinate();
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        var parking = vehicleParkingGroupEntry.getValue();
        var parkingAndGroup = new VehicleParkingAndGroup(group, parking);
        point.setUserData(parkingAndGroup);
        return (Geometry) point;
      })
      .toList();
  }

  enum MapperType {
    Digitransit,
  }
}
