package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static shadow.org.assertj.core.util.Lists.emptyList;

import java.util.List;
import org.junit.jupiter.api.Test;

class BitSetPassThroughPointsTest {

  public static final int STOP_11 = 0;
  public static final int STOP_12 = 1;
  public static final int STOP_13 = 2;
  public static final int[] STOPS_1 = new int[] { STOP_11, STOP_12, STOP_13 };
  public static final int STOP_21 = 3;
  public static final int STOP_22 = 4;
  public static final int STOP_23 = 5;
  public static final int[] STOPS_2 = new int[] { STOP_21, STOP_22, STOP_23 };
  public static final int STOP_31 = 6;
  private static PassThroughPoints PASS_THROUGH_POINTS = BitSetPassThroughPoints.create(
    List.of(STOPS_1, STOPS_2)
  );
  private static PassThroughPoints EMPTY_PASS_THROUGH_POINTS = BitSetPassThroughPoints.create(
    emptyList()
  );

  @Test
  void passThroughPoint() {
    assertTrue(PASS_THROUGH_POINTS.isPassThroughPoint(0, STOP_11));
  }

  @Test
  void passThroughPoint_secondPoint() {
    assertTrue(PASS_THROUGH_POINTS.isPassThroughPoint(1, STOP_22));
  }

  @Test
  void notAPassThroughPoint() {
    assertFalse(PASS_THROUGH_POINTS.isPassThroughPoint(0, STOP_31));
  }

  @Test
  void notAPassThroughPoint_passThroughPointOnIncorrectPosition() {
    assertFalse(PASS_THROUGH_POINTS.isPassThroughPoint(0, STOP_21));
  }

  @Test
  void notAPassThroughPoint_incorrectPassThroughPointIndex() {
    assertFalse(PASS_THROUGH_POINTS.isPassThroughPoint(PASS_THROUGH_POINTS.size(), STOP_11));
  }

  @Test
  void notAPassThroughPoint_empty() {
    assertFalse(EMPTY_PASS_THROUGH_POINTS.isPassThroughPoint(0, STOP_11));
  }
}
