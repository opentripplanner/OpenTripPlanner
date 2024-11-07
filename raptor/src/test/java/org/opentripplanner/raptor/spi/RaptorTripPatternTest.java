package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTripPattern;

public class RaptorTripPatternTest {

  private final TestTripPattern subject = TestTripPattern.pattern("L12", 1, 1, 2, 3, 5, 7, 8, 1);

  @Test
  public void findStopPositionAfter() {
    assertEquals(0, subject.findStopPositionAfter(0, 1));
    assertEquals(1, subject.findStopPositionAfter(1, 1));
    assertEquals(7, subject.findStopPositionAfter(2, 1));
    assertEquals(7, subject.findStopPositionAfter(7, 1));
    assertEquals(3, subject.findStopPositionAfter(0, 3));
  }

  @Test
  public void findStopPositionBefore() {
    assertEquals(0, subject.findStopPositionBefore(0, 1));
    assertEquals(1, subject.findStopPositionBefore(1, 1));
    assertEquals(1, subject.findStopPositionBefore(6, 1));
    assertEquals(7, subject.findStopPositionBefore(7, 1));
    assertEquals(3, subject.findStopPositionBefore(3, 3));
  }
}
