package org.opentripplanner.ext.vectortiles.layers.vehiclerental.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleFuel;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleTypeFactory;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.basic.Ratio;

class DigitransitRentalVehiclePropertyMapperTest {

  private static final String NAME = "a rental";

  @Test
  void noFuel() {
    var mapper = new DigitransitRentalVehiclePropertyMapper();
    var vehicle = builder().build();

    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicle).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("A:B", map.get("id"));
    assertEquals("BICYCLE", map.get("formFactor"));
    assertEquals("HUMAN", map.get("propulsionType"));
    assertNull(map.get("fuelPercentage"));
    assertEquals("A", map.get("network"));
    assertEquals(true, map.get("pickupAllowed"));
    assertNull(map.get("name"));
  }

  @Test
  void fuelPercentage() {
    var mapper = new DigitransitRentalVehiclePropertyMapper();
    var vehicle = builder()
      .withFuel(RentalVehicleFuel.of().withPercent(Ratio.of(0.3)).build())
      .build();

    Map<String, Object> map = new HashMap<>();
    mapper.map(vehicle).forEach(o -> map.put(o.key(), o.value()));

    assertEquals(0.3, map.get("fuelPercentage"));
  }

  private static VehicleRentalVehicle.Builder builder() {
    return VehicleRentalVehicle.of()
      .withId(new FeedScopedId("A", "B"))
      .withLatitude(1)
      .withLongitude(2)
      .withName(I18NString.of(NAME))
      .withVehicleType(RentalVehicleTypeFactory.vehicleType(BICYCLE));
  }
}
