package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

public class DigitransitRentalVehiclePropertyMapper extends PropertyMapper<VehicleRentalVehicle> {

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalVehicle place) {
    return List.of(
      new T2<>("id", place.getId().toString()),
      new T2<>("network", place.getNetwork()),
      new T2<>("formFactor", place.vehicleType.formFactor.toString())
    );
  }
}
