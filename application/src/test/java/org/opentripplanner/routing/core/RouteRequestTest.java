package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class RouteRequestTest {

  private static final Duration DURATION_24_HOURS = Duration.ofHours(24);
  private static final Duration DURATION_24_HOURS_AND_ONE_MINUTE = DURATION_24_HOURS.plusMinutes(1);
  private static final Duration DURATION_ZERO = Duration.ofMinutes(0);
  private static final Duration DURATION_ONE_MINUTE = Duration.ofMinutes(1);
  private static final Duration DURATION_MINUS_ONE_MINUTE = DURATION_ONE_MINUTE.negated();

  @Test
  public void testRequest() {
    // TODO VIA: looks like some parts of this test are obsolete since method no longer exist

    RouteRequest request = new RouteRequest();
    //
    //    request.addMode(CAR);
    //    assertTrue(request.streetSubRequestModes.getCar());
    //    request.removeMode(CAR);
    //    assertFalse(request.streetSubRequestModes.getCar());

    //    request.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK));
    //    assertFalse(request.streetSubRequestModes.getCar());
    //    assertTrue(request.streetSubRequestModes.getBicycle());
    //    assertTrue(request.streetSubRequestModes.getWalk());
  }

  @Test
  public void testIntermediatePlaces() {
    // TODO VIA - Part 2: those methods no longer exist (we will refactor them later). What should we do with this test?
    //    RouteRequest req = new RouteRequest();
    //    assertFalse(req.hasIntermediatePlaces());
    //
    //    req.clearIntermediatePlaces();
    //    assertFalse(req.hasIntermediatePlaces());
    //
    //    req.addIntermediatePlace(randomLocation());
    //    assertTrue(req.hasIntermediatePlaces());
    //
    //    req.clearIntermediatePlaces();
    //    assertFalse(req.hasIntermediatePlaces());
    //
    //    req.addIntermediatePlace(randomLocation());
    //    req.addIntermediatePlace(randomLocation());
    //    assertTrue(req.hasIntermediatePlaces());
  }

  @Test
  public void shouldCloneObjectFields() {
    // TODO VIA (Thomas): There are more objects that are cloned - check freezing
    RouteRequest request = new RouteRequest();

    var clone = request.clone();

    assertNotSame(clone, request);
    assertNotSame(
      clone.journey().transit().raptorDebugging(),
      request.journey().transit().raptorDebugging()
    );
    assertEquals(50, request.numItineraries());
    assertEquals(50, clone.numItineraries());
  }

  @Test
  void testValidateEmptyRequest() {
    RouteRequest request = new RouteRequest();
    try {
      request.validateOriginAndDestination();
      fail();
    } catch (RoutingValidationException e) {
      assertEquals(2, e.getRoutingErrors().size());
    }
  }

  @Test
  void testValidateMissingFrom() {
    RouteRequest request = new RouteRequest();
    request.setTo(randomLocation());
    expectOneRoutingValidationException(
      request::validateOriginAndDestination,
      RoutingErrorCode.LOCATION_NOT_FOUND,
      InputField.FROM_PLACE
    );
  }

  @Test
  void testValidateMissingTo() {
    RouteRequest request = new RouteRequest();
    request.setFrom(randomLocation());
    expectOneRoutingValidationException(
      request::validateOriginAndDestination,
      RoutingErrorCode.LOCATION_NOT_FOUND,
      InputField.TO_PLACE
    );
  }

  @Test
  void testValidateFromAndTo() {
    RouteRequest request = new RouteRequest();
    request.setFrom(randomLocation());
    request.setTo(randomLocation());
    request.validateOriginAndDestination();
  }

  @Test
  void testValidSearchWindow() {
    RouteRequest request = new RouteRequest();
    request.setSearchWindow(DURATION_ONE_MINUTE);
  }

  @Test
  void testZeroSearchWindow() {
    RouteRequest request = new RouteRequest();
    request.setSearchWindow(DURATION_ZERO);
  }

  @Test
  void testTooLongSearchWindow() {
    RouteRequest request = new RouteRequest();
    request.initMaxSearchWindow(DURATION_24_HOURS);
    assertThrows(IllegalArgumentException.class, () ->
      request.setSearchWindow(DURATION_24_HOURS_AND_ONE_MINUTE)
    );
  }

  @Test
  void testNegativeSearchWindow() {
    RouteRequest request = new RouteRequest();
    assertThrows(IllegalArgumentException.class, () ->
      request.setSearchWindow(DURATION_MINUS_ONE_MINUTE)
    );
  }

  @Test
  void allowTransferOptimization() {
    RouteRequest request = new RouteRequest();
    assertTrue(request.allowTransferOptimization());

    request.setViaLocations(
      List.of(new VisitViaLocation("VIA", null, List.of(new FeedScopedId("F", "1")), List.of()))
    );
    assertFalse(request.allowTransferOptimization());
  }

  private GenericLocation randomLocation() {
    return new GenericLocation(Math.random(), Math.random());
  }

  private void expectOneRoutingValidationException(
    Runnable body,
    RoutingErrorCode expCode,
    InputField expField
  ) {
    try {
      body.run();
      fail();
    } catch (RoutingValidationException rve) {
      List<RoutingError> errors = rve.getRoutingErrors();
      assertEquals(1, errors.size());
      assertTrue(errors.stream().anyMatch(e -> e.code == expCode && e.inputField == expField));
    }
  }
}
