package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper.DigitransitVehicleRentalStationPropertyMapper.getFeedScopedIdAndNetwork;

import java.util.ArrayList;
import java.util.Collection;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;

public class DigitransitRentalVehiclePropertyMapper extends PropertyMapper<VehicleRentalVehicle> {

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalVehicle place) {
    var items = new ArrayList<T2<String, Object>>();
    items.addAll(getFeedScopedIdAndNetwork(place));
    items.add(new T2<>("formFactor", place.vehicleType.formFactor.toString()));
    return items;
  }
}
