package org.opentripplanner.ext.smoovebikerental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;

class SmooveBikeRentalDataSourceTest {

  @Test
  void makeStation() {
    SmooveBikeRentalDataSource source = new SmooveBikeRentalDataSource(
      new SmooveBikeRentalDataSourceParameters(
        "file:src/ext-test/resources/smoovebikerental/smoove.json",
        null,
        true,
        HttpHeaders.empty(),
        RentalPickupType.ALL
      )
    );
    assertTrue(source.update());
    List<VehicleRentalPlace> rentalStations = source.getUpdates();

    // Invalid station without coordinates shoulf be ignored, so only 3
    assertEquals(4, rentalStations.size());
    for (VehicleRentalPlace rentalStation : rentalStations) {
      System.out.println(rentalStation);
    }

    VehicleRentalPlace hamn = rentalStations.get(0);
    assertEquals("Hamn", hamn.name().toString());
    assertEquals("A04", hamn.stationId());
    // Ignore whitespace in coordinates string
    assertEquals(24.952269, hamn.longitude());
    assertEquals(60.167913, hamn.latitude());
    assertEquals(11, hamn.spacesAvailable());
    assertEquals(1, hamn.vehiclesAvailable());
    assertTrue(hamn.overloadingAllowed());
    assertTrue(hamn.isAllowDropoff());
    assertTrue(hamn.isAllowPickup());
    assertTrue(hamn.allowDropoffNow());

    VehicleRentalPlace fake = rentalStations.get(1);
    assertEquals("Fake", fake.name().toString());
    assertEquals("B05", fake.stationId());
    assertEquals(24.0, fake.longitude());
    assertEquals(60.0, fake.latitude());
    // operative: false overrides available bikes and slots but not capacity
    assertEquals(0, fake.spacesAvailable());
    assertEquals(0, fake.vehiclesAvailable());
    assertEquals(5, fake.capacity());
    assertFalse(fake.isAllowDropoff());
    assertFalse(fake.isAllowPickup());
    assertFalse(fake.allowDropoffNow());

    VehicleRentalPlace foo = rentalStations.get(2);
    assertEquals("Foo", foo.name().toString());
    assertEquals("B06", foo.stationId());
    assertEquals(25.0, foo.longitude());
    assertEquals(61.0, foo.latitude());
    assertEquals(5, foo.spacesAvailable());
    assertEquals(5, foo.vehiclesAvailable());
    assertEquals(5, foo.capacity());
    assertTrue(foo.allowDropoffNow());
    // Ignores mismatch with total_slots

    VehicleRentalPlace full = rentalStations.get(3);
    assertEquals("Full", full.name().toString());
    assertEquals("B09", full.stationId());
    assertEquals(0, full.spacesAvailable());
    assertEquals(12, full.vehiclesAvailable());
    assertTrue(full.isAllowDropoff());
    assertTrue(full.isAllowPickup());
    assertTrue(full.allowDropoffNow());
  }

  @Test
  void makeStationWithoutOverloading() {
    SmooveBikeRentalDataSource source = new SmooveBikeRentalDataSource(
      new SmooveBikeRentalDataSourceParameters(
        "file:src/ext-test/resources/smoovebikerental/smoove.json",
        null,
        false,
        HttpHeaders.empty(),
        RentalPickupType.ALL
      )
    );
    assertTrue(source.update());
    List<VehicleRentalPlace> rentalStations = source.getUpdates();

    VehicleRentalPlace hamn = rentalStations.get(0);
    assertEquals(11, hamn.spacesAvailable());
    assertFalse(hamn.overloadingAllowed());
    // spaces available and overloading is not allowed
    assertTrue(hamn.allowDropoffNow());

    VehicleRentalPlace fake = rentalStations.get(1);
    assertEquals(0, fake.spacesAvailable());
    // not operative and overloading is not allowed
    assertFalse(fake.allowDropoffNow());

    VehicleRentalPlace full = rentalStations.get(3);
    assertEquals(0, full.spacesAvailable());
    // operative but no spaces and overloading is not allowed
    assertFalse(full.allowDropoffNow());
  }
}
