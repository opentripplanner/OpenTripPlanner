package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;

/**
 * A 10 km leg with a limit of 1500 s and a top speed of 10 m/s. Each kilometre takes at least 100 s,
 * so the leg itself takes at least 1000 s and leaves 500 s for a detour.
 */
class DetourBoundTest {

  private static final WgsCoordinate START = new WgsCoordinate(59.9139, 10.7522);
  private static final DetourBound BOUND = new DetourBound(
    vertexEast(0),
    vertexEast(10_000),
    Duration.ofSeconds(1500),
    10.0
  );

  @Test
  void aPointMayBePassedIfBothBeelinesFitTheLimit() {
    assertTrue(BOUND.mayPassThrough(vertexEast(5_000)));
    // 1300 s there and 300 s back.
    assertFalse(BOUND.mayPassThrough(vertexEast(13_000)));
  }

  @Test
  void aPointMayBePassedNearIfThatSavesEnough() {
    // 1600 s via the point, 100 s over the limit: each metre closer saves 0.2 s.
    assertTrue(BOUND.mayPassWithin(1_000, vertexEast(13_000)));
    assertFalse(BOUND.mayPassWithin(250, vertexEast(13_000)));
  }

  @Test
  void drivenSidesFitIfTheyAddUpToAtMostTheLimit() {
    assertTrue(BOUND.fits(700, 800));
    assertFalse(BOUND.fits(700, 801));
  }

  @Test
  void theEnvelopeHoldsTheFarthestPointADetourCanPass() {
    // 12.4 km there and 2.4 km back is within the 15 km of the limit.
    var farthest = vertexEast(12_400);
    var beyond = vertexEast(13_000);
    var envelope = BOUND.envelope();
    assertTrue(BOUND.mayPassThrough(farthest));
    assertTrue(envelope.contains(farthest.getLon(), farthest.getLat()));
    assertFalse(envelope.contains(beyond.getLon(), beyond.getLat()));
  }

  @Test
  void aSearchFromTheStartAddsTheBeelineOnToTheEnd() {
    // 500 s left to the end.
    assertTrue(BOUND.mayPassThroughAfter(900, vertexEast(5_000)));
    assertFalse(BOUND.mayPassThroughAfter(1100, vertexEast(5_000)));
  }

  @Test
  void aSearchFromTheEndAddsTheBeelineFromTheStart() {
    // 200 s from the start.
    assertTrue(BOUND.mayPassThroughBefore(1200, vertexEast(2_000)));
    assertFalse(BOUND.mayPassThroughBefore(1400, vertexEast(2_000)));
  }

  @Test
  void theLimitOnEachSideOfAPointIsWhatTheBeelineOnTheOtherSideLeaves() {
    // 400 s from the start, 600 s on to the end.
    assertEquals(1100, BOUND.limitFrom(vertexEast(4_000)).toSeconds(), 2);
    assertEquals(900, BOUND.limitTo(vertexEast(4_000)).toSeconds(), 2);
  }

  private static SimpleVertex vertexEast(double meters) {
    var coordinate = START.moveEastMeters(meters);
    return new SimpleVertex("v" + meters, coordinate.latitude(), coordinate.longitude());
  }
}
