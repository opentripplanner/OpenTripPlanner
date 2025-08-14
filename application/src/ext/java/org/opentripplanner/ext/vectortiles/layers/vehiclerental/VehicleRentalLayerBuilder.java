package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;

abstract class VehicleRentalLayerBuilder<T extends VehicleRentalPlace> extends LayerBuilder<T> {

  private final VehicleRentalService service;

  public VehicleRentalLayerBuilder(
    VehicleRentalService service,
    Map<MapperType, PropertyMapper<T>> mappers,
    LayerParameters<VectorTilesResource.LayerType> layerParameters
  ) {
    super(
      mappers.get(MapperType.valueOf(layerParameters.mapper())),
      layerParameters.name(),
      layerParameters.expansionFactor()
    );
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
        Coordinate coordinate = new Coordinate(rental.longitude(), rental.latitude());
        Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate);
        point.setUserData(rental);
        return (Geometry) point;
      })
      .toList();
  }

  protected abstract Collection<T> getVehicleRentalPlaces(VehicleRentalService service);

  enum MapperType {
    Digitransit,
    DigitransitRealtime,
  }
}
