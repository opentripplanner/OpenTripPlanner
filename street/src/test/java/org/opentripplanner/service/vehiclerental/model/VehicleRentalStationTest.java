package org.opentripplanner.service.vehiclerental.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.RentalFormFactor;

class VehicleRentalStationTest {

  private static final RentalVehicleType BICYCLE = RentalVehicleType.DEFAULT;
  private static final RentalFormFactor BICYCLE_FORM = RentalFormFactor.BICYCLE;

  @Test
  void allowPickupNow_openStationWithVehicles() {
    var station = VehicleRentalStation.of().withIsRenting(true).withVehiclesAvailable(3).build();
    assertTrue(station.allowPickupNow());
  }

  @Test
  void allowPickupNow_openStationNoVehicles() {
    var station = VehicleRentalStation.of().withIsRenting(true).withVehiclesAvailable(0).build();
    assertFalse(station.allowPickupNow());
  }

  @Test
  void allowPickupNow_closedStation() {
    var station = VehicleRentalStation.of().withIsRenting(false).withVehiclesAvailable(5).build();
    assertFalse(station.allowPickupNow());
  }

  @Test
  void allowDropoffNow_openStationWithSpaces() {
    var station = VehicleRentalStation.of().withIsReturning(true).withSpacesAvailable(3).build();
    assertTrue(station.allowDropoffNow());
  }

  @Test
  void allowDropoffNow_openStationNoSpaces() {
    var station = VehicleRentalStation.of().withIsReturning(true).withSpacesAvailable(0).build();
    assertFalse(station.allowDropoffNow());
  }

  @Test
  void allowDropoffNow_closedStationWithSpaces() {
    var station = VehicleRentalStation.of().withIsReturning(false).withSpacesAvailable(5).build();
    assertFalse(station.allowDropoffNow());
  }

  @Test
  void allowDropoffNow_overloadingAllowedNoSpaces() {
    var station = VehicleRentalStation.of()
      .withIsReturning(true)
      .withSpacesAvailable(0)
      .withOverloadingAllowed(true)
      .build();
    assertTrue(station.allowDropoffNow());
  }

  @Test
  void allowDropoffNow_overloadingAllowedButClosed() {
    var station = VehicleRentalStation.of()
      .withIsReturning(false)
      .withSpacesAvailable(0)
      .withOverloadingAllowed(true)
      .build();
    assertFalse(station.allowDropoffNow());
  }

  @Test
  void canDropOffFormFactor_anyTypeWithRealtimeAndSpaces() {
    var station = VehicleRentalStation.of()
      .withIsReturning(true)
      .withSpacesAvailable(3)
      .withReturnPolicy(ReturnPolicy.ANY_TYPE)
      .build();
    assertTrue(station.canDropOffFormFactor(BICYCLE_FORM, true));
  }

  @Test
  void canDropOffFormFactor_anyTypeWithRealtimeClosedStation() {
    var station = VehicleRentalStation.of()
      .withIsReturning(false)
      .withSpacesAvailable(5)
      .withReturnPolicy(ReturnPolicy.ANY_TYPE)
      .build();
    assertFalse(station.canDropOffFormFactor(BICYCLE_FORM, true));
  }

  @Test
  void canDropOffFormFactor_anyTypeNoRealtimeIgnoresClosedStation() {
    var station = VehicleRentalStation.of()
      .withIsReturning(false)
      .withSpacesAvailable(0)
      .withReturnPolicy(ReturnPolicy.ANY_TYPE)
      .build();
    assertTrue(station.canDropOffFormFactor(BICYCLE_FORM, false));
  }

  @Test
  void canDropOffFormFactor_specificTypesSpacesAvailable() {
    var station = VehicleRentalStation.of()
      .withVehicleSpacesAvailable(Map.of(BICYCLE, 3))
      .withReturnPolicy(ReturnPolicy.SPECIFIC_TYPES)
      .build();
    assertTrue(station.canDropOffFormFactor(BICYCLE_FORM, true));
  }

  @Test
  void canDropOffFormFactor_specificTypesNoSpacesForType() {
    var station = VehicleRentalStation.of()
      .withVehicleSpacesAvailable(Map.of(BICYCLE, 0))
      .withReturnPolicy(ReturnPolicy.SPECIFIC_TYPES)
      .build();
    assertFalse(station.canDropOffFormFactor(BICYCLE_FORM, true));
  }
}
