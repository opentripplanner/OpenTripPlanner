package org.opentripplanner.ext.taxi.model;

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

class TaxiLegTest implements PlanTestConstants {

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
  void modeIsTaxi() {
    var leg = taxiLeg();
    assertThat(leg.mode()).isEqualTo(TransitMode.TAXI);
  }

  @Test
  void agencyComesFromTaxiRoute() {
    var leg = taxiLeg();
    assertThat(leg.agency()).isEqualTo(ROUTE.getAgency());
  }

  @Test
  void routeComesFromTaxiRoute() {
    var leg = taxiLeg();
    assertThat(leg.route()).isEqualTo(ROUTE);
  }

  @Test
  void isTransitLeg() {
    var leg = taxiLeg();
    assertThat(leg.isTransitLeg()).isFalse();
  }

  @Test
  void isStreetLeg() {
    var leg = taxiLeg();
    assertThat(leg.isStreetLeg()).isTrue();
  }

  @Test
  void listTransitAlertsIsEmpty() {
    var leg = taxiLeg();
    assertThat(leg.listTransitAlerts()).isEmpty();
  }

  @Test
  void serviceDateIsNull() {
    var leg = taxiLeg();
    assertThat(leg.serviceDate()).isNull();
  }

  @Test
  void boardAndAlightStopPositionsAreFixed() {
    var leg = taxiLeg();
    assertThat(leg.boardStopPosInPattern()).isEqualTo(0);
    assertThat(leg.alightStopPosInPattern()).isEqualTo(1);
  }

  @Test
  void bookingInfoComesFromTaxiRoute() {
    var leg = taxiLeg();
    assertThat(leg.pickupBookingInfo()).isEqualTo(PICKUP_BOOKING_INFO);
    assertThat(leg.dropOffBookingInfo()).isEqualTo(DROP_OFF_BOOKING_INFO);
  }

  @Test
  void withEmissionPerPersonRetainsType() {
    var leg = taxiLeg();
    var updated = leg.withEmissionPerPerson(Emission.ofCo2Gram(5));
    assertThat(updated).isInstanceOf(TaxiLeg.class);
    var castLeg = (TaxiLeg) updated;
    assertThat(castLeg.taxiRoute()).isEqualTo(leg.taxiRoute());
    assertThat(castLeg.emissionPerPerson()).isEqualTo(Emission.ofCo2Gram(5));
  }

  @Test
  void withTimeShiftRetainsType() {
    var leg = taxiLeg();
    var updated = leg.withTimeShift(Duration.ofMinutes(5));
    assertThat(updated).isInstanceOf(TaxiLeg.class);
    var castLeg = (TaxiLeg) updated;
    assertThat(castLeg.taxiRoute()).isEqualTo(leg.taxiRoute());
  }

  @Test
  void fareOffersIsEmpty() {
    var leg = taxiLeg();
    assertThat(leg.fareOffers()).isEmpty();
  }

  @Test
  void accessibilityScoreIsNull() {
    var leg = taxiLeg();
    assertThat(leg.accessibilityScore()).isNull();
  }

  @Test
  void rentalFieldsAreNotUsed() {
    var leg = taxiLeg();
    assertThat(leg.walkingBike()).isFalse();
    assertThat(leg.rentedVehicle()).isFalse();
    assertThat(leg.vehicleRentalNetwork()).isNull();
  }

  @Test
  void hasSameModeTrueForMatchingTaxiLeg() {
    var leg = taxiLeg();
    var other = taxiLeg();
    assertThat(leg.hasSameMode(other)).isTrue();
  }

  @Test
  void hasSameModeFalseForPlainStreetLeg() {
    var leg = taxiLeg();
    assertThat(leg.hasSameMode(driveLeg())).isFalse();
  }

  private static StreetLeg driveLeg() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    return (StreetLeg) itinerary.legs().getFirst();
  }

  private static TaxiLeg taxiLeg() {
    var taxiRoute = new TaxiRoute(ROUTE, Polygons.OSLO, PICKUP_BOOKING_INFO, DROP_OFF_BOOKING_INFO);
    return new TaxiLeg(driveLeg(), taxiRoute);
  }
}
