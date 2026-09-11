package org.opentripplanner.ext.taxizone.model;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Polygons;
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

  @Test
  void modeComesFromZoneRoute() {
    var leg = taxiZoneLeg();
    assertThat(leg.mode()).isEqualTo(TransitMode.TAXI);
  }

  @Test
  void agencyComesFromZoneRoute() {
    var leg = taxiZoneLeg();
    assertThat(leg.agency()).isEqualTo(ROUTE.getAgency());
  }

  @Test
  void routeComesFromZone() {
    var leg = taxiZoneLeg();
    assertThat(leg.route()).isEqualTo(ROUTE);
  }

  @Test
  void isTransitLeg() {
    var leg = taxiZoneLeg();
    assertThat(leg.isTransitLeg()).isFalse();
  }

  @Test
  void isStreetLeg() {
    var leg = taxiZoneLeg();
    assertThat(leg.isStreetLeg()).isTrue();
  }

  @Test
  void listTransitAlertsIsEmpty() {
    var leg = taxiZoneLeg();
    assertThat(leg.listTransitAlerts()).isEmpty();
  }

  @Test
  void serviceDateComesFromLegStartTime() {
    var leg = taxiZoneLeg();
    assertThat(leg.serviceDate()).isEqualTo(leg.startTime().toLocalDate());
  }

  @Test
  void boardAndAlightStopPositionsAreFixed() {
    var leg = taxiZoneLeg();
    assertThat(leg.boardStopPosInPattern()).isEqualTo(0);
    assertThat(leg.alightStopPosInPattern()).isEqualTo(1);
  }

  @Test
  void bookingInfoComesFromZone() {
    var leg = taxiZoneLeg();
    assertThat(leg.pickupBookingInfo()).isEqualTo(PICKUP_BOOKING_INFO);
    assertThat(leg.dropOffBookingInfo()).isEqualTo(DROP_OFF_BOOKING_INFO);
  }

  @Test
  void withEmissionPerPersonRetainsType() {
    var leg = taxiZoneLeg();
    var updated = leg.withEmissionPerPerson(Emission.ofCo2Gram(5));
    assertThat(updated).isInstanceOf(TaxiZoneLeg.class);
    var castLeg = (TaxiZoneLeg) updated;
    assertThat(castLeg.taxiZone()).isEqualTo(leg.taxiZone());
    assertThat(castLeg.emissionPerPerson()).isEqualTo(Emission.ofCo2Gram(5));
  }

  @Test
  void withTimeShiftRetainsType() {
    var leg = taxiZoneLeg();
    var updated = leg.withTimeShift(Duration.ofMinutes(5));
    assertThat(updated).isInstanceOf(TaxiZoneLeg.class);
    var castLeg = (TaxiZoneLeg) updated;
    assertThat(castLeg.taxiZone()).isEqualTo(leg.taxiZone());
  }

  @Test
  void hasSameModeTrueForMatchingTaxiZoneLeg() {
    var leg = taxiZoneLeg();
    var other = taxiZoneLeg();
    assertThat(leg.hasSameMode(other)).isTrue();
  }

  @Test
  void hasSameModeFalseForPlainStreetLeg() {
    var leg = taxiZoneLeg();
    assertThat(leg.hasSameMode(driveLeg())).isFalse();
  }

  @Test
  void hasSameModeFalseForDifferentModeZoneLeg() {
    var leg = taxiZoneLeg();
    var otherRoute = TransitRepositoryForTest.route("other-route")
      .withMode(TransitMode.CARPOOL)
      .build();
    var otherZone = new TaxiZone(Polygons.OSLO, otherRoute, null, null);
    var other = new TaxiZoneLeg(driveLeg(), otherZone);
    assertThat(leg.hasSameMode(other)).isFalse();
  }

  private static StreetLeg driveLeg() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    return (StreetLeg) itinerary.legs().getFirst();
  }

  private static TaxiZoneLeg taxiZoneLeg() {
    var zone = new TaxiZone(Polygons.OSLO, ROUTE, PICKUP_BOOKING_INFO, DROP_OFF_BOOKING_INFO);
    return new TaxiZoneLeg(driveLeg(), zone);
  }
}
