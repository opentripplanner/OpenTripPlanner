package org.opentripplanner.ext.taxizone.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

class TaxiZoneLegTest implements PlanTestConstants {

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

  private static TaxiZoneLeg taxiZoneLeg() {
    var zone = new TaxiZone(
      null,
      ROUTE,
      PICKUP_BOOKING_INFO,
      DROP_OFF_BOOKING_INFO,
      LocalDateRange.ofUnbounded()
    );
    return new TaxiZoneLeg(driveLeg(), zone);
  }

  @Test
  void modeComesFromZoneRoute() {
    var leg = taxiZoneLeg();
    assertEquals(TransitMode.TAXI, leg.mode());
  }

  @Test
  void agencyComesFromZoneRoute() {
    var leg = taxiZoneLeg();
    assertEquals(ROUTE.getAgency(), leg.agency());
  }

  @Test
  void routeComesFromZone() {
    var leg = taxiZoneLeg();
    assertEquals(ROUTE, leg.route());
  }

  @Test
  void isTransitLeg() {
    var leg = taxiZoneLeg();
    assertFalse(leg.isTransitLeg());
  }

  @Test
  void isStreetLeg() {
    var leg = taxiZoneLeg();
    assertTrue(leg.isStreetLeg());
  }

  @Test
  void listTransitAlertsIsEmpty() {
    var leg = taxiZoneLeg();
    assertTrue(leg.listTransitAlerts().isEmpty());
  }

  @Test
  void serviceDateComesFromLegStartTime() {
    var leg = taxiZoneLeg();
    assertEquals(leg.startTime().toLocalDate(), leg.serviceDate());
  }

  @Test
  void boardAndAlightStopPositionsAreFixed() {
    var leg = taxiZoneLeg();
    assertEquals(0, leg.boardStopPosInPattern());
    assertEquals(1, leg.alightStopPosInPattern());
  }

  @Test
  void bookingInfoComesFromZone() {
    var leg = taxiZoneLeg();
    assertEquals(PICKUP_BOOKING_INFO, leg.pickupBookingInfo());
    assertEquals(DROP_OFF_BOOKING_INFO, leg.dropOffBookingInfo());
  }

  @Test
  void withEmissionPerPersonRetainsType() {
    var leg = taxiZoneLeg();
    var updated = leg.withEmissionPerPerson(Emission.ofCo2Gram(5));
    var castLeg = assertInstanceOf(TaxiZoneLeg.class, updated);
    assertEquals(leg.taxiZone(), castLeg.taxiZone());
    assertEquals(Emission.ofCo2Gram(5), castLeg.emissionPerPerson());
  }

  @Test
  void withTimeShiftRetainsType() {
    var leg = taxiZoneLeg();
    var updated = leg.withTimeShift(Duration.ofMinutes(5));
    var castLeg = assertInstanceOf(TaxiZoneLeg.class, updated);
    assertEquals(leg.taxiZone(), castLeg.taxiZone());
  }

  @Test
  void hasSameModeTrueForMatchingTaxiZoneLeg() {
    var leg = taxiZoneLeg();
    var other = taxiZoneLeg();
    assertTrue(leg.hasSameMode(other));
  }

  @Test
  void hasSameModeFalseForPlainStreetLeg() {
    var leg = taxiZoneLeg();
    assertFalse(leg.hasSameMode(driveLeg()));
  }

  @Test
  void hasSameModeFalseForDifferentModeZoneLeg() {
    var leg = taxiZoneLeg();
    var otherRoute = TransitRepositoryForTest.route("other-route")
      .withMode(TransitMode.CARPOOL)
      .build();
    var otherZone = new TaxiZone(null, otherRoute, null, null, LocalDateRange.ofUnbounded());
    var other = new TaxiZoneLeg(driveLeg(), otherZone);
    assertFalse(leg.hasSameMode(other));
  }
}
