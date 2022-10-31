package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import static java.util.Map.entry;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitRealtimeVehicleRentalStationPropertyMapper;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;

public class VehicleRentalStationsLayerBuilder
  extends VehicleRentalLayerBuilder<VehicleRentalStation> {

  public VehicleRentalStationsLayerBuilder(
    VehicleRentalStationService service,
    VectorTilesResource.LayerParameters layerParameters,
    Locale locale
  ) {
    super(
      service,
      Map.ofEntries(
        entry(MapperType.Digitransit, new DigitransitVehicleRentalStationPropertyMapper(locale)),
        entry(
          MapperType.DigitransitRealtime,
          new DigitransitRealtimeVehicleRentalStationPropertyMapper(locale)
        )
      ),
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
