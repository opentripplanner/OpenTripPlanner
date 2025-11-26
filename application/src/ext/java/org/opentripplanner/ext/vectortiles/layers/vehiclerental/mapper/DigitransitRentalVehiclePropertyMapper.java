package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper.getFeedScopedIdAndNetwork;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.ArrayList;
import java.util.Collection;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;

public class DigitransitRentalVehiclePropertyMapper extends PropertyMapper<VehicleRentalVehicle> {

  @Override
  protected Collection<KeyValue> map(VehicleRentalVehicle vehicle) {
    var items = new ArrayList<KeyValue>();
    items.addAll(getFeedScopedIdAndNetwork(vehicle));
    items.add(kv("formFactor", vehicle.vehicleType().formFactor()));
    items.add(kv("propulsionType", vehicle.vehicleType().propulsionType()));
    if (vehicle.fuel() != null && vehicle.fuel().percent() != null) {
      items.add(kv("fuelPercentage", vehicle.fuel().percent().asDouble()));
    }
    items.add(kv("pickupAllowed", vehicle.isAllowPickup()));
    return items;
  }
}
