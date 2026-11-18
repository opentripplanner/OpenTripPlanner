package org.opentripplanner.service.vehiclerental.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.RentalFormFactor;

class VehicleRentalStationTest {

  private static final RentalVehicleType BICYCLE = RentalVehicleType.DEFAULT;
  private static final RentalFormFactor BICYCLE_FORM = RentalFormFactor.BICYCLE;

  private static final VehicleRentalStation SUBJECT = createSubject();

  @Test
  void testEqualsAndHashCode() {
    var same = createSubject();

    assertEquals(SUBJECT, same);
    assertEquals(SUBJECT.hashCode(), same.hashCode());
  }

  private static Stream<Arguments> equalsAndHashCodeFieldCases() {
    return Stream.of(
      fieldCase("id", b -> b.withId(new FeedScopedId("feed", "other"))),
      fieldCase("name", b -> b.withName(new NonLocalizedString("Other"))),
      fieldCase("longitude", b -> b.withLongitude(25.0)),
      fieldCase("latitude", b -> b.withLatitude(61.0)),
      fieldCase("capacity", b -> b.withCapacity(11)),
      fieldCase("vehicleTypeAreaCapacity", b -> b.withVehicleTypeAreaCapacity(Map.of(BICYCLE, 6))),
      fieldCase("vehicleTypeDockCapacity", b -> b.withVehicleTypeDockCapacity(Map.of(BICYCLE, 8))),
      fieldCase("system", b ->
        b.withSystem(VehicleRentalSystem.of().withSystemId("other").build())
      ),
      fieldCase("rentalUris", b ->
        b.withRentalUris(VehicleRentalStationUris.of().withWeb("https://other").build())
      ),
      fieldCase("vehiclesAvailable", b -> b.withVehiclesAvailable(4)),
      fieldCase("vehiclesDisabled", b -> b.withVehiclesDisabled(2)),
      fieldCase("vehicleTypesAvailable", b -> b.withVehicleTypesAvailable(Map.of(BICYCLE, 4))),
      fieldCase("spacesAvailable", b -> b.withSpacesAvailable(6)),
      fieldCase("spacesDisabled", b -> b.withSpacesDisabled(2)),
      fieldCase("vehicleSpacesAvailable", b -> b.withVehicleSpacesAvailable(Map.of(BICYCLE, 6))),
      fieldCase("isRenting", b -> b.withIsRenting(false)),
      fieldCase("isReturning", b -> b.withIsReturning(false)),
      fieldCase("overloadingAllowed", b -> b.withOverloadingAllowed(false)),
      fieldCase("isArrivingInRentalVehicleAtDestinationAllowed", b ->
        b.withIsArrivingInRentalVehicleAtDestinationAllowed(false)
      ),
      fieldCase("realTimeData", b -> b.withRealTimeData(true)),
      fieldCase("returnPolicy", b -> b.withReturnPolicy(ReturnPolicy.SPECIFIC_TYPES))
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("equalsAndHashCodeFieldCases")
  void fieldIsPartOfEqualsAndHashCode(String field, Consumer<VehicleRentalStationBuilder> change) {
    var builder = SUBJECT.copyOf();
    change.accept(builder);
    var other = builder.build();

    assertNotEquals(SUBJECT, other);
    assertNotEquals(SUBJECT.hashCode(), other.hashCode());
  }

  private static Arguments fieldCase(String field, Consumer<VehicleRentalStationBuilder> change) {
    return Arguments.of(field, change);
  }

  private static VehicleRentalStation createSubject() {
    return VehicleRentalStation.of()
      .withId(new FeedScopedId("feed", "station"))
      .withName(new NonLocalizedString("Station"))
      .withLongitude(24.9)
      .withLatitude(60.2)
      .withCapacity(10)
      .withVehicleTypeAreaCapacity(Map.of(BICYCLE, 5))
      .withVehicleTypeDockCapacity(Map.of(BICYCLE, 7))
      .withSystem(VehicleRentalSystem.of().withSystemId("system").build())
      .withRentalUris(VehicleRentalStationUris.of().withWeb("https://station").build())
      .withVehiclesAvailable(3)
      .withVehiclesDisabled(1)
      .withVehicleTypesAvailable(Map.of(BICYCLE, 3))
      .withSpacesAvailable(5)
      .withSpacesDisabled(1)
      .withVehicleSpacesAvailable(Map.of(BICYCLE, 5))
      .withIsRenting(true)
      .withIsReturning(true)
      .withOverloadingAllowed(true)
      .withIsArrivingInRentalVehicleAtDestinationAllowed(true)
      .withRealTimeData(false)
      .withReturnPolicy(ReturnPolicy.ANY_TYPE)
      .build();
  }

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
