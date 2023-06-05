package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;

class RouteRequestTest {

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
      request.validate();
      Assertions.fail();
    } catch (RoutingValidationException e) {
      Assertions.assertEquals(2, e.getRoutingErrors().size());
    }
  }

  @Test
  void testValidateMissingFrom() {
    RouteRequest request = new RouteRequest();
    request.setTo(randomLocation());
    try {
      request.validate();
      fail();
    } catch (RoutingValidationException e) {
      List<RoutingError> routingErrors = e.getRoutingErrors();
      Assertions.assertEquals(1, routingErrors.size());
      Assertions.assertTrue(
        routingErrors
          .stream()
          .anyMatch(routingError ->
            routingError.code == RoutingErrorCode.LOCATION_NOT_FOUND &&
            routingError.inputField == InputField.FROM_PLACE
          )
      );
    }
  }

  @Test
  void testValidateMissingTo() {
    RouteRequest request = new RouteRequest();
    request.setFrom(randomLocation());
    try {
      request.validate();
    } catch (RoutingValidationException e) {
      List<RoutingError> routingErrors = e.getRoutingErrors();

      Assertions.assertEquals(1, routingErrors.size());
      Assertions.assertTrue(
        routingErrors
          .stream()
          .anyMatch(routingError ->
            routingError.code == RoutingErrorCode.LOCATION_NOT_FOUND &&
            routingError.inputField == InputField.TO_PLACE
          )
      );
    }
  }

  @Test
  void testValidateFromAndTo() {
    RouteRequest request = new RouteRequest();
    request.setFrom(randomLocation());
    request.setTo(randomLocation());
    request.validate();
  }

  private GenericLocation randomLocation() {
    return new GenericLocation(Math.random(), Math.random());
  }
}
