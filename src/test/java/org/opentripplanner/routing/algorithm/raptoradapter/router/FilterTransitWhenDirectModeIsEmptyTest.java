package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

public class FilterTransitWhenDirectModeIsEmptyTest {

  @Test
  public void directModeIsExistAndIsNotWalking() {
    var modes = RequestModes
      .of()
      .withAccessMode(null)
      .withEgressMode(null)
      .withDirectMode(StreetMode.BIKE)
      .withTransferMode(null)
      .clearTransitMode()
      .build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.BIKE, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.BIKE, subject.originalDirectMode());
  }

  @Test
  public void directModeIsExistAndIsWalking() {
    var modes = RequestModes.of().withDirectMode(StreetMode.WALK).clearTransitMode().build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.WALK, subject.originalDirectMode());
  }

  @Test
  public void directModeIsEmpty() {
    var modes = RequestModes
      .of()
      .withAccessMode(null)
      .withEgressMode(null)
      .withDirectMode(null)
      .withTransferMode(null)
      .clearTransitMode()
      .build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertTrue(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.NOT_SET, subject.originalDirectMode());
  }
}
