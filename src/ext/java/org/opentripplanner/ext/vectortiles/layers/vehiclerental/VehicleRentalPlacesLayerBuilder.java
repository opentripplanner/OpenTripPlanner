package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalPropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;

public class VehicleRentalPlacesLayerBuilder extends VehicleRentalLayerBuilder<VehicleRentalPlace> {

  public VehicleRentalPlacesLayerBuilder(
    VehicleRentalStationService service,
    VectorTilesResource.LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      service,
      Map.of(MapperType.Digitransit, new DigitransitVehicleRentalPropertyMapper(locale)),
      layerParameters
    );
  }

  @Override
  protected Collection<VehicleRentalPlace> getVehicleRentalPlaces(
    VehicleRentalStationService service
  ) {
    return service.getVehicleRentalPlaces();
  }
}
