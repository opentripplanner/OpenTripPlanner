package org.opentripplanner.inspector.vector.rental;

import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.utils.collection.ListUtils;

class RentalPropertyMapper extends PropertyMapper<VehicleRentalPlace> {

  @Override
  protected Collection<KeyValue> map(VehicleRentalPlace input) {
    var base = List.of(
      kv("class", input.getClass().getSimpleName()),
      kv("id", input.getId()),
      kv("network", input.getNetwork()),
      kv("vehiclesAvailable", input.getVehiclesAvailable()),
      kv("spacesAvailable", input.getSpacesAvailable())
    );

    if (input instanceof VehicleRentalVehicle vehicle) {
      var props = List.of(
        kv("formFactor", vehicle.vehicleType.formFactor),
        kv("isReserved", vehicle.isReserved),
        kv("isDisabled", vehicle.isDisabled)
      );
      return ListUtils.combine(base, props);
    } else if (input instanceof VehicleRentalStation station) {
      var props = List.of(
        kv("isRenting", station.isRenting),
        kv("isReturning", station.isReturning)
      );
      return ListUtils.combine(base, props);
    }
    return base;
  }
}
