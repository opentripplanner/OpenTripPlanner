package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import static java.util.Map.entry;

import java.util.Collection;
import java.util.Map;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitRentalVehiclePropertyMapper;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalPropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

public class VehicleRentalVehiclesLayerBuilder
  extends VehicleRentalLayerBuilder<VehicleRentalVehicle> {

  public VehicleRentalVehiclesLayerBuilder(
    VehicleRentalStationService service,
    VectorTilesResource.LayerParameters layerParameters
  ) {
    super(
      service,
      Map.ofEntries(
        entry(MapperType.Digitransit, new DigitransitRentalVehiclePropertyMapper()),
        entry(MapperType.DigitransitRealtime, new DigitransitRentalVehiclePropertyMapper())
      ),
      layerParameters
    );
  }

  @Override
  protected Collection<VehicleRentalVehicle> getVehicleRentalPlaces(
    VehicleRentalStationService service
  ) {
    return service.getVehicleRentalVehicles();
  }
}
