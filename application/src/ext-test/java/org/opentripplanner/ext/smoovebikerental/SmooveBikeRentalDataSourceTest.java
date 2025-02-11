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
    assertEquals("Hamn", hamn.getName().toString());
    assertEquals("A04", hamn.getStationId());
    // Ignore whitespace in coordinates string
    assertEquals(24.952269, hamn.getLongitude());
    assertEquals(60.167913, hamn.getLatitude());
    assertEquals(11, hamn.getSpacesAvailable());
    assertEquals(1, hamn.getVehiclesAvailable());
    assertTrue(hamn.overloadingAllowed());
    assertTrue(hamn.isAllowDropoff());
    assertTrue(hamn.isAllowPickup());
    assertTrue(hamn.allowDropoffNow());

    VehicleRentalPlace fake = rentalStations.get(1);
    assertEquals("Fake", fake.getName().toString());
    assertEquals("B05", fake.getStationId());
    assertEquals(24.0, fake.getLongitude());
    assertEquals(60.0, fake.getLatitude());
    // operative: false overrides available bikes and slots but not capacity
    assertEquals(0, fake.getSpacesAvailable());
    assertEquals(0, fake.getVehiclesAvailable());
    assertEquals(5, fake.getCapacity());
    assertFalse(fake.isAllowDropoff());
    assertFalse(fake.isAllowPickup());
    assertFalse(fake.allowDropoffNow());

    VehicleRentalPlace foo = rentalStations.get(2);
    assertEquals("Foo", foo.getName().toString());
    assertEquals("B06", foo.getStationId());
    assertEquals(25.0, foo.getLongitude());
    assertEquals(61.0, foo.getLatitude());
    assertEquals(5, foo.getSpacesAvailable());
    assertEquals(5, foo.getVehiclesAvailable());
    assertEquals(5, foo.getCapacity());
    assertTrue(foo.allowDropoffNow());
    // Ignores mismatch with total_slots

    VehicleRentalPlace full = rentalStations.get(3);
    assertEquals("Full", full.getName().toString());
    assertEquals("B09", full.getStationId());
    assertEquals(0, full.getSpacesAvailable());
    assertEquals(12, full.getVehiclesAvailable());
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
    assertEquals(11, hamn.getSpacesAvailable());
    assertFalse(hamn.overloadingAllowed());
    // spaces available and overloading is not allowed
    assertTrue(hamn.allowDropoffNow());

    VehicleRentalPlace fake = rentalStations.get(1);
    assertEquals(0, fake.getSpacesAvailable());
    // not operative and overloading is not allowed
    assertFalse(fake.allowDropoffNow());

    VehicleRentalPlace full = rentalStations.get(3);
    assertEquals(0, full.getSpacesAvailable());
    // operative but no spaces and overloading is not allowed
    assertFalse(full.allowDropoffNow());
  }
}
