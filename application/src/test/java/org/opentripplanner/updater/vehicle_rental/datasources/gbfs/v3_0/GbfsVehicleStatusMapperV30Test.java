package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3_0;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.vehicle_status.GBFSVehicle;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GbfsVehicleStatusMapperV30Test {

  public static final VehicleRentalSystem SYSTEM = new VehicleRentalSystem(
    "123",
    I18NString.of("123"),
    I18NString.of("123"),
    I18NString.of("123"),
    "https://example.com"
  );
  public static final GbfsVehicleStatusMapperV30 MAPPER = new GbfsVehicleStatusMapperV30(
    SYSTEM,
    Map.of(
      "scooter",
      new RentalVehicleType(
        new FeedScopedId("1", "scooter"),
        I18NString.of("Scooter"),
        RentalFormFactor.SCOOTER,
        RentalVehicleType.PropulsionType.COMBUSTION,
        null
      )
    )
  );

  @Test
  void noType() {
    var vehicle = new GBFSVehicle();
    vehicle.setVehicleId("999999999");
    vehicle.setLat(1d);
    vehicle.setLon(1d);
    var mapped = MAPPER.mapVehicleStatus(vehicle);

    assertEquals("Default vehicle type", mapped.name().toString());
  }

  @Test
  void withDefaultType() {
    var vehicle = new GBFSVehicle();
    vehicle.setVehicleId("999999999");
    vehicle.setLat(1d);
    vehicle.setLon(1d);
    vehicle.setVehicleTypeId("bike");
    var mapped = MAPPER.mapVehicleStatus(vehicle);
    assertEquals("Default vehicle type", mapped.name().toString());
  }

  @Test
  void withType() {
    var vehicle = new GBFSVehicle();
    vehicle.setVehicleId("999999999");
    vehicle.setLat(1d);
    vehicle.setLon(1d);
    vehicle.setVehicleTypeId("scooter");
    vehicle.setCurrentRangeMeters(2000d);
    var mapped = MAPPER.mapVehicleStatus(vehicle);

    assertEquals("Scooter", mapped.name().toString());
  }
}
