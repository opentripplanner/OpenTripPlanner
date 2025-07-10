package org.opentripplanner.inspector.vector.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;

class RentalPropertyMapperTest {

  private static final RentalPropertyMapper MAPPER = new RentalPropertyMapper();

  @Test
  void mapStation() {
    var station = TestVehicleRentalStationBuilder.of().build();
    var result = MAPPER.map(station);
    assertEquals(
      List.of(
        kv("class", "VehicleRentalStation"),
        kv("id", "Network-1:FooStation"),
        kv("network", "Network-1"),
        kv("vehiclesAvailable", 10),
        kv("spacesAvailable", 10),
        kv("isRenting", false),
        kv("isReturning", false)
      ),
      result
    );
  }

  @Test
  void mapVehicle() {
    var vehicle = TestFreeFloatingRentalVehicleBuilder.of().build();
    var result = MAPPER.map(vehicle);
    assertEquals(
      List.of(
        kv("class", "VehicleRentalVehicle"),
        kv("id", "Network-1:free-floating-bicycle"),
        kv("network", "Network-1"),
        kv("vehiclesAvailable", 1),
        kv("spacesAvailable", 0),
        kv("formFactor", "BICYCLE"),
        kv("isReserved", false),
        kv("isDisabled", false)
      ),
      result
    );
  }
}
