package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;

// TODO: 2022-12-06 filters: fix tests
public class FilterTransitWhenDirectModeIsEmptyTest {

//  @Test
//  public void directModeIsExistAndIsNotWalking() {
//    var modes = RequestModes
//      .of()
//      .withAccessMode(null)
//      .withEgressMode(null)
//      .withDirectMode(StreetMode.BIKE)
//      .withTransferMode(null)
//      .clearTransitModes()
//      .build();
//
//    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);
//
//    assertEquals(StreetMode.BIKE, subject.resolveDirectMode());
//    assertFalse(subject.removeWalkAllTheWayResults());
//    assertEquals(StreetMode.BIKE, subject.originalDirectMode());
//  }
//
//  @Test
//  public void directModeIsExistAndIsWalking() {
//    var modes = RequestModes.of().withDirectMode(StreetMode.WALK).clearTransitModes().build();
//
//    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);
//
//    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
//    assertFalse(subject.removeWalkAllTheWayResults());
//    assertEquals(StreetMode.WALK, subject.originalDirectMode());
//  }
//
//  @Test
//  public void directModeIsEmpty() {
//    var modes = RequestModes
//      .of()
//      .withAccessMode(null)
//      .withEgressMode(null)
//      .withDirectMode(null)
//      .withTransferMode(null)
//      .clearTransitModes()
//      .build();
//
//    var subject = new FilterTransitWhenDirectModeIsEmpty(modes);
//
//    assertEquals(StreetMode.WALK, subject.resolveDirectMode());
//    assertTrue(subject.removeWalkAllTheWayResults());
//    assertEquals(StreetMode.NOT_SET, subject.originalDirectMode());
//  }
}
