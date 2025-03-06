package org.opentripplanner.service.vehiclerental.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class DefaultVehicleRentalServiceTest {

  @Test
  void getVehicleRentalStationForEnvelopeShouldExcludeVehicleRentalVehicle() {
    DefaultVehicleRentalService defaultVehicleRentalService = new DefaultVehicleRentalService();

    VehicleRentalStation vehicleRentalStation = new TestVehicleRentalStationBuilder()
      .withCoordinates(1, 1)
      .build();
    defaultVehicleRentalService.addVehicleRentalStation(vehicleRentalStation);

    VehicleRentalVehicle vehicleRentalVehicle = new TestFreeFloatingRentalVehicleBuilder()
      .withLatitude(2)
      .withLongitude(2)
      .build();
    defaultVehicleRentalService.addVehicleRentalStation(vehicleRentalVehicle);

    List<VehicleRentalStation> vehicleRentalStationForEnvelope =
      defaultVehicleRentalService.getVehicleRentalStationForEnvelope(0, 0, 10, 10);
    assertEquals(1, vehicleRentalStationForEnvelope.size());
    assertEquals(vehicleRentalStation, vehicleRentalStationForEnvelope.get(0));
  }
}
