package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTripPattern;

public class RaptorTripPatternTest implements RaptorTestConstants {

  /// Note! Even if the restrictions allow alighting at stop 0 and boarding at the last stop,
  /// the general rule that you are not allowed to board at the last stop and alight at the first
  /// should still be enforced.
  private TestTripPattern subject = TestTripPattern.of("L12", 1, 1, 1, 1, 8, 1, 1, 1, 1)
    .restrictions("* b - a * b - a *")
    .build();
  private int n = subject.numberOfStopsInPattern();

  @Test
  public void findBoardStopPositionAfter() {
    int[] expected = { 0, 1, 5, 5, 5, 5, -1, -1, -1 };
    assertNumberOfExpectedResultsMatchesPattern(expected, 0);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], subject.findBoardStopPositionAfter(i, 1), "i=" + i);
    }

    var ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findBoardStopPositionAfter(-1, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 8]: -1", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findBoardStopPositionAfter(n, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 8]: 9", ex.getMessage());
  }

  @Test
  public void findBoardStopPositionBefore() {
    int[] expected = { -1, 0, 1, 1, 1, 1, 5, 5, 5 };
    assertNumberOfExpectedResultsMatchesPattern(expected, 0);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], subject.findBoardStopPositionBefore(i, 1), "i=" + i);
    }

    var ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findBoardStopPositionBefore(-1, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 8]: -1", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findBoardStopPositionBefore(n, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 8]: 9", ex.getMessage());
  }

  @Test
  public void findAlightStopPositionAfter() {
    int[] expected = { 3, 3, 3, 7, 7, 7, 7, 8, -1 };
    assertNumberOfExpectedResultsMatchesPattern(expected, 0);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], subject.findAlightStopPositionAfter(i, 1), "i=" + i);
    }

    var ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findAlightStopPositionAfter(-1, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 8]: -1", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findAlightStopPositionAfter(n, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 8]: 9", ex.getMessage());
  }

  @Test
  public void findAlightStopPositionBefore() {
    int delta = 1;
    int[] expected = { -1, -1, -1, -1, 3, 3, 3, 3, 7, 8 };
    assertNumberOfExpectedResultsMatchesPattern(expected, delta);

    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], subject.findAlightStopPositionBefore(i, 1), "i=" + i);
    }

    var ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findAlightStopPositionBefore(-1, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 9]: -1", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
      subject.findAlightStopPositionBefore(n + delta, 1)
    );
    assertEquals("The 'startPos' is not in range[0, 9]: 10", ex.getMessage());
  }

  private void assertNumberOfExpectedResultsMatchesPattern(int[] expected, int delta) {
    assertEquals(
      subject.numberOfStopsInPattern() + delta,
      expected.length,
      "Number of of expected results matches pattern"
    );
  }
}
