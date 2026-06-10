package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;

class CarpoolingRequestTest {

  private static final WgsCoordinate PICKUP = new WgsCoordinate(59.91, 10.75);
  private static final WgsCoordinate DROPOFF = new WgsCoordinate(59.93, 10.78);
  private static final Instant DATE_TIME = Instant.parse("2024-06-01T10:00:00Z");

  @Test
  void of_routeRequest_mapsAllFields() {
    var request = buildRouteRequest(true);
    var carpoolingRequest = CarpoolingRequest.of(request);

    assertTrue(carpoolingRequest.isArriveByRequest());
    assertEquals(DATE_TIME, carpoolingRequest.getRequestedDateTime());
    assertEquals(PICKUP, carpoolingRequest.getPassengerPickup());
    assertEquals(DROPOFF, carpoolingRequest.getPassengerDropoff());
    assertFalse(carpoolingRequest.isAccessEgressRequest());
  }

  @ParameterizedTest
  @EnumSource(AccessEgressType.class)
  void of_routeRequestWithAccessOrEgress_flagsRoutingMode(AccessEgressType type) {
    var carpoolingRequest = CarpoolingRequest.of(buildRouteRequest(false), type);

    assertTrue(carpoolingRequest.isAccessEgressRequest());
    assertEquals(type.isAccess(), carpoolingRequest.isAccessRequest());
    assertEquals(type.isEgress(), carpoolingRequest.isEgressRequest());
  }

  @Test
  void accessEgressBooleans_areFalseForDirectRouting() {
    var carpoolingRequest = CarpoolingRequest.of(buildRouteRequest(false));

    assertFalse(carpoolingRequest.isAccessEgressRequest());
    assertFalse(carpoolingRequest.isAccessRequest());
    assertFalse(carpoolingRequest.isEgressRequest());
  }

  private static RouteRequest buildRouteRequest(boolean arriveBy) {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(PICKUP.latitude(), PICKUP.longitude()))
      .withTo(GenericLocation.fromCoordinate(DROPOFF.latitude(), DROPOFF.longitude()))
      .withDateTime(DATE_TIME)
      .withArriveBy(arriveBy)
      .buildRequest();
  }
}
