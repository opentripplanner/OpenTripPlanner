package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import org.junit.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

public class FilterTransitWhenDirectModeIsEmptyTest {

  @Test
  public void directModeIsExistAndIsNotWalking() {
    var modes = new RequestModes(null, null, null, StreetMode.BIKE, Set.of());

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.BIKE, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.BIKE, subject.originalDirectMode());
  }

  @Test
  public void directModeIsExistAndIsWalking() {
    var modes = new RequestModes(null, null, null, StreetMode.WALK, Set.of());

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.WALK, subject.originalDirectMode());
  }

  @Test
  public void directModeIsEmpty() {
    var modes = new RequestModes(null, null, null, null, Set.of());

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertTrue(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.NOT_SET, subject.originalDirectMode());
  }
}
