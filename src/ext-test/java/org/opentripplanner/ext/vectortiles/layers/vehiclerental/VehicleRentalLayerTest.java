package org.opentripplanner.ext.vectortiles.layers.vehiclerental;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalVehicle;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleRentalLayerTest {

  public static final String NAME = "a rental station";
  DigitransitVehicleRentalPropertyMapper mapper = new DigitransitVehicleRentalPropertyMapper();

  @Test
  public void floatingVehicle() {
    var vehicle = new VehicleRentalVehicle();
    vehicle.id = new FeedScopedId("A", "B");
    vehicle.latitude = 1;
    vehicle.longitude = 2;
    vehicle.name = new NonLocalizedString(NAME);
    vehicle.vehicleType =
      new RentalVehicleType(
        new FeedScopedId("1", "2"),
        "bicycle",
        RentalVehicleType.FormFactor.BICYCLE,
        RentalVehicleType.PropulsionType.HUMAN,
        1000d
      );

    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicle).forEach(o -> map.put(o.first, o.second));

    assertEquals("bicycle", map.get("formFactors"));
    assertEquals(NAME, map.get("name"));
    assertEquals("floatingVehicle", map.get("type"));
    assertEquals("A", map.get("networks"));
  }
}
