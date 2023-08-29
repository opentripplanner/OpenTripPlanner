package org.opentripplanner.service.vehiclerental.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class DefaultVehicleRentalServiceTest {

  @Test
  void getVehicleRentalStationForEnvelopeShouldExcludeVehicleRentalVehicle() {
    DefaultVehicleRentalService defaultVehicleRentalService = new DefaultVehicleRentalService();

    VehicleRentalStation vehicleRentalStation = new VehicleRentalStation();
    vehicleRentalStation.id = new FeedScopedId("Feed1", "VehicleRentalStation1");
    vehicleRentalStation.latitude = 1;
    vehicleRentalStation.longitude = 1;
    defaultVehicleRentalService.addVehicleRentalStation(vehicleRentalStation);

    VehicleRentalVehicle vehicleRentalVehicle = new VehicleRentalVehicle();
    vehicleRentalVehicle.id = new FeedScopedId("Feed1", "VehicleRentalVehicle1");
    vehicleRentalVehicle.latitude = 2;
    vehicleRentalVehicle.longitude = 2;
    defaultVehicleRentalService.addVehicleRentalStation(vehicleRentalVehicle);

    List<VehicleRentalPlace> vehicleRentalStationForEnvelope = defaultVehicleRentalService.getVehicleRentalStationForEnvelope(
      0,
      0,
      10,
      10
    );
    assertEquals(1, vehicleRentalStationForEnvelope.size());
    assertEquals(vehicleRentalStation, vehicleRentalStationForEnvelope.get(0));
  }
}
