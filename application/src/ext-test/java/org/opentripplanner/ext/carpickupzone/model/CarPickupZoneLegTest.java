package org.opentripplanner.ext.carpickupzone.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

class CarPickupZoneLegTest implements PlanTestConstants {

  private static final Route ROUTE = TransitRepositoryForTest.route("taxi-route")
    .withMode(TransitMode.TAXI)
    .build();

  private static final Place PLACE_A = Place.forStop(
    TEST_MODEL.stop("A").withCoordinate(5.0, 8.0).build()
  );
  private static final Place PLACE_B = Place.forStop(
    TEST_MODEL.stop("B").withCoordinate(6.0, 8.5).build()
  );

  private static final BookingInfo PICKUP_BOOKING_INFO = BookingInfo.of().build();
  private static final BookingInfo DROP_OFF_BOOKING_INFO = BookingInfo.of().build();

  private static StreetLeg driveLeg() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    return (StreetLeg) itinerary.legs().getFirst();
  }

  private static CarPickupZoneLeg carPickupZoneLeg() {
    var zone = new CarPickupZone(null, ROUTE, PICKUP_BOOKING_INFO, DROP_OFF_BOOKING_INFO);
    return new CarPickupZoneLeg(driveLeg(), zone);
  }

  @Test
  void modeComesFromZoneRoute() {
    var leg = carPickupZoneLeg();
    assertEquals(TransitMode.TAXI, leg.mode());
  }

  @Test
  void agencyComesFromZoneRoute() {
    var leg = carPickupZoneLeg();
    assertEquals(ROUTE.getAgency(), leg.agency());
  }

  @Test
  void routeComesFromZone() {
    var leg = carPickupZoneLeg();
    assertEquals(ROUTE, leg.route());
  }

  @Test
  void bookingInfoComesFromZone() {
    var leg = carPickupZoneLeg();
    assertEquals(PICKUP_BOOKING_INFO, leg.pickupBookingInfo());
    assertEquals(DROP_OFF_BOOKING_INFO, leg.dropOffBookingInfo());
  }

  @Test
  void withEmissionPerPersonRetainsType() {
    var leg = carPickupZoneLeg();
    var updated = leg.withEmissionPerPerson(Emission.ofCo2Gram(5));
    var carPickupLeg = assertInstanceOf(CarPickupZoneLeg.class, updated);
    assertEquals(leg.carPickupZone(), carPickupLeg.carPickupZone());
    assertEquals(Emission.ofCo2Gram(5), carPickupLeg.emissionPerPerson());
  }

  @Test
  void withAccessibilityScoreRetainsType() {
    var leg = carPickupZoneLeg();
    var updated = leg.withAccessibilityScore(0.5f);
    var carPickupLeg = assertInstanceOf(CarPickupZoneLeg.class, updated);
    assertEquals(leg.carPickupZone(), carPickupLeg.carPickupZone());
    assertEquals(0.5f, carPickupLeg.accessibilityScore());
  }

  @Test
  void withTimeShiftRetainsType() {
    var leg = carPickupZoneLeg();
    var updated = leg.withTimeShift(Duration.ofMinutes(5));
    var carPickupLeg = assertInstanceOf(CarPickupZoneLeg.class, updated);
    assertEquals(leg.carPickupZone(), carPickupLeg.carPickupZone());
  }

  @Test
  void hasSameModeTrueForMatchingCarPickupZoneLeg() {
    var leg = carPickupZoneLeg();
    var other = carPickupZoneLeg();
    assertTrue(leg.hasSameMode(other));
  }

  @Test
  void hasSameModeFalseForPlainStreetLeg() {
    var leg = carPickupZoneLeg();
    assertFalse(leg.hasSameMode(driveLeg()));
  }

  @Test
  void hasSameModeFalseForDifferentModeZoneLeg() {
    var leg = carPickupZoneLeg();
    var otherRoute = TransitRepositoryForTest.route("other-route")
      .withMode(TransitMode.CARPOOL)
      .build();
    var otherZone = new CarPickupZone(null, otherRoute, null, null);
    var other = new CarPickupZoneLeg(driveLeg(), otherZone);
    assertFalse(leg.hasSameMode(other));
  }
}
