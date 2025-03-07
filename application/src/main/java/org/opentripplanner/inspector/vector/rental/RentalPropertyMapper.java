package org.opentripplanner.inspector.vector.rental;

import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;

public class RentalPropertyMapper extends PropertyMapper<VehicleRentalPlace> {

  @Override
  protected Collection<KeyValue> map(VehicleRentalPlace input) {
    return List.of(
      kv("class", input.getClass().getSimpleName()),
      kv("id", input.getName()),
      kv("network", input.getNetwork()),
      kv("vehiclesAvailable", input.getVehiclesAvailable()),
      kv("spacesAvailable", input.getSpacesAvailable())
    );
  }
}
