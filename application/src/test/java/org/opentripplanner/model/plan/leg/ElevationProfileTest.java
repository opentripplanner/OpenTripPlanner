package org.opentripplanner.model.plan.leg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.leg.ElevationProfile.Step;

class ElevationProfileTest {

  private static final double ANY = 98.12;
  private static final double ZERO = 0;
  private static final double SEVEN = 7.00;
  private static final double SEVEN_HIGH = 7.0049;
  private static final double SEVEN_LOW = 6.995;

  @Test
  void stepX() {
    assertEquals(ZERO, new Step(ZERO, ANY).x());
    assertEquals(SEVEN, new Step(SEVEN_HIGH, ANY).x());
    assertEquals(SEVEN, new Step(SEVEN_LOW, ANY).x());
  }

  @Test
  void setpY() {
    assertEquals(ZERO, new Step(ANY, ZERO).y());
    assertEquals(SEVEN, new Step(ANY, SEVEN_HIGH).y());
    assertEquals(SEVEN, new Step(ANY, SEVEN_LOW).y());
  }

  @Test
  void offsetX() {
    var subject = ElevationProfile.of().step(ZERO, ANY).transformX(2.0).build();
    assertEquals(2.0, subject.steps().get(0).x());
    assertEquals(ANY, subject.steps().get(0).y());
  }

  @Test
  void isEmpty() {
    assertTrue(ElevationProfile.empty().isEmpty());
    assertTrue(ElevationProfile.of().build().isEmpty());
    assertFalse(ElevationProfile.of().step(ANY, ANY).build().isEmpty());
  }

  @Test
  void isAllYUnknown() {
    assertTrue(ElevationProfile.of().stepYUnknown(ANY).build().isAllYUnknown());
    assertFalse(ElevationProfile.empty().isAllYUnknown());
    assertFalse(ElevationProfile.of().stepYUnknown(ANY).step(ANY, ANY).build().isAllYUnknown());
  }

  @Test
  void steps() {
    assertEquals(
      List.of(new Step(ANY, ZERO)),
      ElevationProfile.of().step(ANY, ZERO).build().steps()
    );
  }

  @Test
  void removeDuplicateSteps() {
    var p = ElevationProfile.of()
      .step(0, 0)
      .step(0, 0)
      .step(1, 1)
      .step(2, 1)
      .step(3, 1)
      .stepYUnknown(4)
      .stepYUnknown(4)
      .stepYUnknown(5)
      .step(5, Double.NaN)
      .stepYUnknown(6)
      .stepYUnknown(7)
      .step(8, 10)
      .step(8, 10)
      .build();

    assertEquals(
      "ElevationProfile{steps: [[0.0, 0.0], [1.0, 1.0], [3.0, 1.0], [4.0, UNKNOWN], [7.0, UNKNOWN], [8.0, 10.0]]}",
      p.toString()
    );
  }

  @Test
  void testToString() {
    var p = ElevationProfile.of().step(0, 0).step(2.347, 1.561).build();
    assertEquals("ElevationProfile{steps: [[0.0, 0.0], [2.35, 1.56]]}", p.toString());
    assertEquals("ElevationProfile{}", ElevationProfile.empty().toString());
  }

  @Test
  void testEqualsAndHashCode() {
    var p0 = ElevationProfile.of().step(ZERO, ZERO).build();
    var p1 = ElevationProfile.of().step(SEVEN_LOW, SEVEN_LOW).build();
    var p2 = ElevationProfile.of().step(SEVEN_HIGH, SEVEN_HIGH).build();

    // Same instance
    assertEquals(p1, p1);
    assertEquals(p1.hashCode(), p1.hashCode());

    // Same value
    assertEquals(p1, p2);
    assertEquals(p1.hashCode(), p2.hashCode());

    // Different
    assertNotEquals(p0, p1);
    assertNotEquals(p0.hashCode(), p1.hashCode());
  }

  @Test
  void assertCopyOfIsSameAsOriginal() {
    var p = ElevationProfile.of().step(ZERO, ZERO).build();
    assertSame(p, p.copyOf().build());
  }

  @Test
  void calculateElevationChange() {
    var p = ElevationProfile.of()
      .step(1, 10.0)
      .step(2, 11.0)
      .step(3, 10.5)
      .step(4, 11.5)
      .step(5, 11.5)
      .step(6, 11.2)
      .build();
    assertEquals(2.0, p.elevationGained());
    assertEquals(0.8, p.elevationLost());
  }

  @Test
  void add() {
    var p0 = ElevationProfile.of().step(1, 10.0).step(2, 11.0).build();
    var p1 = ElevationProfile.of().step(3, 10.5).step(4, 11.2).step(5, 13.1).build();

    assertEquals(
      "ElevationProfile{steps: [[1.0, 10.0], [2.0, 11.0], [3.0, 10.5], [4.0, 11.2], [5.0, 13.1]]}",
      p0.add(p1).toString()
    );
  }
}
