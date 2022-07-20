package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.core.TraverseMode.CAR;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RoutingRequest;

public class RoutingRequestTest {

  @Test
  public void testRequest() {
    RoutingRequest request = new RoutingRequest();

    request.addMode(CAR);
    assertTrue(request.streetSubRequestModes.getCar());
    request.removeMode(CAR);
    assertFalse(request.streetSubRequestModes.getCar());

    request.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK));
    assertFalse(request.streetSubRequestModes.getCar());
    assertTrue(request.streetSubRequestModes.getBicycle());
    assertTrue(request.streetSubRequestModes.getWalk());
  }

  @Test
  public void testIntermediatePlaces() {
    RoutingRequest req = new RoutingRequest();
    assertFalse(req.hasIntermediatePlaces());

    req.clearIntermediatePlaces();
    assertFalse(req.hasIntermediatePlaces());

    req.addIntermediatePlace(randomLocation());
    assertTrue(req.hasIntermediatePlaces());

    req.clearIntermediatePlaces();
    assertFalse(req.hasIntermediatePlaces());

    req.addIntermediatePlace(randomLocation());
    req.addIntermediatePlace(randomLocation());
    assertTrue(req.hasIntermediatePlaces());
  }

  @Test
  public void shouldCloneObjectFields() {
    var req = new RoutingRequest();

    var clone = req.clone();

    assertNotSame(clone, req);
    assertNotSame(clone.itineraryFilters, req.itineraryFilters);
    assertNotSame(clone.raptorDebugging, req.raptorDebugging);
    assertNotSame(clone.raptorOptions, req.raptorOptions);

    assertEquals(50, req.numItineraries);
    assertEquals(50, clone.numItineraries);
  }

  private GenericLocation randomLocation() {
    return new GenericLocation(Math.random(), Math.random());
  }
}
