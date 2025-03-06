package org.opentripplanner.ext.vectortiles.layers.vehicleparkings;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;

public class VehicleParkingGroupsLayerBuilder extends LayerBuilder<VehicleParkingAndGroup> {

  static Map<
    VehicleParkingGroupsLayerBuilder.MapperType,
    Function<Locale, PropertyMapper<VehicleParkingAndGroup>>
  > mappers = Map.of(
    VehicleParkingGroupsLayerBuilder.MapperType.Digitransit,
    DigitransitVehicleParkingGroupPropertyMapper::create
  );
  private final VehicleParkingService service;

  public VehicleParkingGroupsLayerBuilder(
    VehicleParkingService service,
    LayerParameters<VectorTilesResource.LayerType> layerParameters,
    Locale locale
  ) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())).apply(locale),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
    this.service = service;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    return service
      .listVehicleParkingGroups()
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
