package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.ext.vectortiles.LayerBuilder;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.util.geometry.GeometryUtils;

abstract class VehicleRentalLayerBuilder<T extends VehicleRentalPlace> extends LayerBuilder<T> {

  private final VehicleRentalStationService service;

  public VehicleRentalLayerBuilder(
    VehicleRentalStationService service,
    Map<MapperType, PropertyMapper<T>> mappers,
    VectorTilesResource.LayerParameters layerParameters
  ) {
    super(layerParameters.name(), mappers.get(MapperType.valueOf(layerParameters.mapper())));
    this.service = service;
  }

  @Override
  protected List<Geometry> getGeometries(Envelope query) {
    if (service == null) {
      return List.of();
    }
    return getVehicleRentalPlaces(service)
      .stream()
      .map(rental -> {
        Coordinate coordinate = new Coordinate(rental.getLongitude(), rental.getLatitude());
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(rental);
        return (Geometry) point;
      })
      .toList();
  }

  protected abstract Collection<T> getVehicleRentalPlaces(VehicleRentalStationService service);

  enum MapperType {
    Digitransit,
  }
}
