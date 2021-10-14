package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.graph.Graph;

import java.util.Collection;
import java.util.List;

public class DigitransitVehicleRentalPropertyMapper extends PropertyMapper<VehicleRentalPlace> {
  public static DigitransitVehicleRentalPropertyMapper create(Graph graph) {
    return new DigitransitVehicleRentalPropertyMapper();
  }

  @Override
  protected Collection<T2<String, Object>> map(VehicleRentalPlace station) {
    return List.of(
        new T2<>("id", station.getStationId()),
        new T2<>("name", station.getName()),
        new T2<>("networks", station.getNetwork())
    );
  }
}
