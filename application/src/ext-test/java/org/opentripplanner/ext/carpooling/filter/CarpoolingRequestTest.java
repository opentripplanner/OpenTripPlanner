package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;

class CarpoolingRequestTest {

  private static final WgsCoordinate PICKUP = new WgsCoordinate(59.91, 10.75);
  private static final WgsCoordinate DROPOFF = new WgsCoordinate(59.93, 10.78);
  private static final Instant DATE_TIME = Instant.parse("2024-06-01T10:00:00Z");

  @Test
  void of_routeRequest_mapsArriveByFalse() {
    var request = buildRouteRequest(PICKUP, DROPOFF, DATE_TIME, false);
    var carpoolingRequest = CarpoolingRequest.of(request);

    assertFalse(carpoolingRequest.isArriveByRequest());
  }

  @Test
  void of_routeRequest_mapsArriveByTrue() {
    var request = buildRouteRequest(PICKUP, DROPOFF, DATE_TIME, true);
    var carpoolingRequest = CarpoolingRequest.of(request);

    assertTrue(carpoolingRequest.isArriveByRequest());
  }

  @Test
  void of_routeRequest_mapsDateTime() {
    var request = buildRouteRequest(PICKUP, DROPOFF, DATE_TIME, false);
    var carpoolingRequest = CarpoolingRequest.of(request);

    assertEquals(DATE_TIME, carpoolingRequest.getRequestedDateTime());
  }

  @Test
  void of_routeRequest_mapsPassengerPickupAndDropoff() {
    var request = buildRouteRequest(PICKUP, DROPOFF, DATE_TIME, false);
    var carpoolingRequest = CarpoolingRequest.of(request);

    assertEquals(PICKUP, carpoolingRequest.getPassengerPickup());
    assertEquals(DROPOFF, carpoolingRequest.getPassengerDropoff());
  }

  @Test
  void of_routeRequest_accessOrEgressIsNull() {
    var request = buildRouteRequest(PICKUP, DROPOFF, DATE_TIME, false);
    var carpoolingRequest = CarpoolingRequest.of(request);

    assertNull(carpoolingRequest.getAccessOrEgress());
  }

  @Test
  void of_routeRequestWithAccess_setsAccessOrEgress() {
    var request = buildRouteRequest(PICKUP, DROPOFF, DATE_TIME, false);
    var carpoolingRequest = CarpoolingRequest.of(request, AccessEgressType.ACCESS);

    assertEquals(AccessEgressType.ACCESS, carpoolingRequest.getAccessOrEgress());
  }

  @Test
  void of_routeRequestWithEgress_setsAccessOrEgress() {
    var request = buildRouteRequest(PICKUP, DROPOFF, DATE_TIME, false);
    var carpoolingRequest = CarpoolingRequest.of(request, AccessEgressType.EGRESS);

    assertEquals(AccessEgressType.EGRESS, carpoolingRequest.getAccessOrEgress());
  }

  @Test
  void isAccessEgressRequest_accessOrEgressIsNull_returnsFalse() {
    var request = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);

    assertFalse(request.isAccessEgressRequest());
  }

  @Test
  void isAccessEgressRequest_accessOrEgressIsAccess_returnsTrue() {
    var request = buildCarpoolingRequest(
      false,
      PICKUP,
      DROPOFF,
      DATE_TIME,
      AccessEgressType.ACCESS
    );

    assertTrue(request.isAccessEgressRequest());
  }

  @Test
  void isAccessEgressRequest_accessOrEgressIsEgress_returnsTrue() {
    var request = buildCarpoolingRequest(
      false,
      PICKUP,
      DROPOFF,
      DATE_TIME,
      AccessEgressType.EGRESS
    );

    assertTrue(request.isAccessEgressRequest());
  }

  @Test
  void isAccessRequest_accessOrEgressIsNull_returnsFalse() {
    var request = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);

    assertFalse(request.isAccessRequest());
  }

  @Test
  void isAccessRequest_accessOrEgressIsAccess_returnsTrue() {
    var request = buildCarpoolingRequest(
      false,
      PICKUP,
      DROPOFF,
      DATE_TIME,
      AccessEgressType.ACCESS
    );

    assertTrue(request.isAccessRequest());
  }

  @Test
  void isAccessRequest_accessOrEgressIsEgress_returnsFalse() {
    var request = buildCarpoolingRequest(
      false,
      PICKUP,
      DROPOFF,
      DATE_TIME,
      AccessEgressType.EGRESS
    );

    assertFalse(request.isAccessRequest());
  }

  @Test
  void isEgressRequest_accessOrEgressIsNull_returnsFalse() {
    var request = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);

    assertFalse(request.isEgressRequest());
  }

  @Test
  void isEgressRequest_accessOrEgressIsAccess_returnsFalse() {
    var request = buildCarpoolingRequest(
      false,
      PICKUP,
      DROPOFF,
      DATE_TIME,
      AccessEgressType.ACCESS
    );

    assertFalse(request.isEgressRequest());
  }

  @Test
  void isEgressRequest_accessOrEgressIsEgress_returnsTrue() {
    var request = buildCarpoolingRequest(
      false,
      PICKUP,
      DROPOFF,
      DATE_TIME,
      AccessEgressType.EGRESS
    );

    assertTrue(request.isEgressRequest());
  }

  @Test
  void equals_sameFields_returnsTrue() {
    var a = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);
    var b = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void equals_differentArriveBy_returnsFalse() {
    var a = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);
    var b = buildCarpoolingRequest(true, PICKUP, DROPOFF, DATE_TIME, null);

    assertFalse(a.equals(b));
  }

  @Test
  void equals_differentDateTime_returnsFalse() {
    var a = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);
    var b = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME.plusSeconds(1), null);

    assertFalse(a.equals(b));
  }

  @Test
  void equals_differentPickup_returnsFalse() {
    var a = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);
    var b = buildCarpoolingRequest(false, new WgsCoordinate(1.0, 1.0), DROPOFF, DATE_TIME, null);

    assertFalse(a.equals(b));
  }

  @Test
  void equals_differentDropoff_returnsFalse() {
    var a = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, null);
    var b = buildCarpoolingRequest(false, PICKUP, new WgsCoordinate(1.0, 1.0), DATE_TIME, null);

    assertFalse(a.equals(b));
  }

  @Test
  void equals_differentAccessOrEgress_returnsFalse() {
    var a = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, AccessEgressType.ACCESS);
    var b = buildCarpoolingRequest(false, PICKUP, DROPOFF, DATE_TIME, AccessEgressType.EGRESS);

    assertFalse(a.equals(b));
  }

  private static RouteRequest buildRouteRequest(
    WgsCoordinate from,
    WgsCoordinate to,
    Instant dateTime,
    boolean arriveBy
  ) {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(from.latitude(), from.longitude()))
      .withTo(GenericLocation.fromCoordinate(to.latitude(), to.longitude()))
      .withDateTime(dateTime)
      .withArriveBy(arriveBy)
      .buildRequest();
  }

  private static CarpoolingRequest buildCarpoolingRequest(
    boolean arriveBy,
    WgsCoordinate pickup,
    WgsCoordinate dropoff,
    Instant dateTime,
    AccessEgressType accessOrEgress
  ) {
    return new CarpoolingRequestBuilder()
      .withArriveBy(arriveBy)
      .withPassengerPickup(pickup)
      .withPassengerDropoff(dropoff)
      .withRequestedDateTime(dateTime)
      .withAccessOrEgress(accessOrEgress)
      .build();
  }
}
