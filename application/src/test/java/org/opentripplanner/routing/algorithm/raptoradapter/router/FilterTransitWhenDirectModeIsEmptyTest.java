package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

public class FilterTransitWhenDirectModeIsEmptyTest {

  @Test
  public void directModeExistsAndIsNotWalking() {
    var modes = RequestModes.of()
      .withAccessMode(null)
      .withEgressMode(null)
      .withDirectMode(StreetMode.BIKE)
      .withTransferMode(null)
      .build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes, false);

    assertEquals(StreetMode.BIKE, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.BIKE, subject.originalDirectMode());
  }

  @Test
  public void directModeExistsAndIsWalking() {
    var modes = RequestModes.of().withDirectMode(StreetMode.WALK).build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes, false);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.WALK, subject.originalDirectMode());
  }

  @Test
  public void directModeIsEmpty() {
    var modes = RequestModes.of()
      .withAccessMode(null)
      .withEgressMode(null)
      .withDirectMode(null)
      .withTransferMode(null)
      .build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes, false);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertTrue(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.NOT_SET, subject.originalDirectMode());
  }

  @Test
  public void directModeIsEmptyAndPageCursorExists() {
    var modes = RequestModes.of()
      .withAccessMode(null)
      .withEgressMode(null)
      .withDirectMode(null)
      .withTransferMode(null)
      .build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes, true);

    assertEquals(StreetMode.NOT_SET, subject.resolveDirectMode());
    assertTrue(!subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.NOT_SET, subject.originalDirectMode());
  }

  @Test
  public void directModeExistsAndIsWalkingAndPageCursorExists() {
    var modes = RequestModes.of().withDirectMode(StreetMode.WALK).build();

    var subject = new FilterTransitWhenDirectModeIsEmpty(modes, true);

    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
    assertFalse(subject.removeWalkAllTheWayResults());
    assertEquals(StreetMode.WALK, subject.originalDirectMode());
  }
}
