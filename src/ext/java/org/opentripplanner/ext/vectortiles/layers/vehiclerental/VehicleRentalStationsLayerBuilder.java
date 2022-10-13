package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import java.util.Collection;
import java.util.Map;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;

public class VehicleRentalStationsLayerBuilder
  extends VehicleRentalLayerBuilder<VehicleRentalStation> {

  public VehicleRentalStationsLayerBuilder(
    VehicleRentalStationService service,
    VectorTilesResource.LayerParameters layerParameters
  ) {
    super(
      service,
      Map.of(MapperType.Digitransit, new DigitransitVehicleRentalStationPropertyMapper()),
      layerParameters
    );
  }

  @Override
  protected Collection<VehicleRentalStation> getVehicleRentalPlaces(
    VehicleRentalStationService service
  ) {
    return service.getVehicleRentalStations();
  }
}
