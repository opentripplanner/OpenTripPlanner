package org.opentripplanner.updater.vehicle_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v2_3.free_bike_status.GBFSBike;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GbfsFreeVehicleStatusMapperTest {

  public static final VehicleRentalSystem SYSTEM = new VehicleRentalSystem(
    "123",
    "de",
    "123",
    "123",
    "123",
    "https://example.com",
    "https://example.com",
    null,
    null,
    "help@foo.com",
    "hello@foo.com",
    "Europe/Berlin",
    null,
    null,
    null
  );
  public static final GbfsFreeVehicleStatusMapper MAPPER = new GbfsFreeVehicleStatusMapper(
    SYSTEM,
    Map.of(
      "scooter",
      new RentalVehicleType(
        new FeedScopedId("1", "scooter"),
        "Scooter",
        RentalFormFactor.SCOOTER,
        RentalVehicleType.PropulsionType.COMBUSTION,
        null
      )
    )
  );

  @Test
  void noType() {
    var bike = new GBFSBike();
    bike.setBikeId("999999999");
    bike.setLat(1d);
    bike.setLon(1d);
    var mapped = MAPPER.mapFreeVehicleStatus(bike);

    assertEquals("Default vehicle type", mapped.name.toString());
  }

  @Test
  void withDefaultType() {
    var bike = new GBFSBike();
    bike.setBikeId("999999999");
    bike.setLat(1d);
    bike.setLon(1d);
    bike.setVehicleTypeId("bike");
    var mapped = MAPPER.mapFreeVehicleStatus(bike);
    assertEquals("Default vehicle type", mapped.name.toString());
  }

  @Test
  void withType() {
    var bike = new GBFSBike();
    bike.setBikeId("999999999");
    bike.setLat(1d);
    bike.setLon(1d);
    bike.setVehicleTypeId("scooter");
    bike.setCurrentRangeMeters(2000d);
    var mapped = MAPPER.mapFreeVehicleStatus(bike);

    assertEquals("Scooter", mapped.name.toString());
  }
}
