package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.raptor._data.RaptorTestConstants.SECONDS_IN_A_DAY;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import org.junit.jupiter.api.Test;

class AccessWithPenaltyTest {

  private static final int STOP = 17;
  private static final int DURATION = 400;
  private static final int PENALTY = 100;
  private static final int OPEN = 600;
  private static final int CLOSE = 1800;
  private static final int ANY_TIME = 500;
  private static final int EXPECTED_OPENING = OPEN - PENALTY;
  private static final int EXPECTED_CLOSING = CLOSE - PENALTY;
  private static final int ONE_DAY = SECONDS_IN_A_DAY;

  @Test
  void durationInSeconds() {
    var subject = new AccessWithPenalty(walk(STOP, DURATION).withTimePenalty(PENALTY));
    assertEquals(DURATION + PENALTY, subject.durationInSeconds());
  }

  @Test
  void earliestDepartureTimeIsBeforeOpeningHours() {
    var subject = new AccessWithPenalty(
      walk(STOP, DURATION).openingHours(OPEN, CLOSE).withTimePenalty(PENALTY)
    );

    assertEquals(EXPECTED_OPENING, subject.earliestDepartureTime(EXPECTED_OPENING - 200));
    assertEquals(EXPECTED_OPENING, subject.earliestDepartureTime(EXPECTED_OPENING - 1));
  }

  @Test
  void earliestDepartureTimeIsInsideOpeningHours() {
    var subject = new AccessWithPenalty(
      walk(STOP, DURATION).openingHours(OPEN, CLOSE).withTimePenalty(PENALTY)
    );

    assertEquals(EXPECTED_OPENING, subject.earliestDepartureTime(EXPECTED_OPENING));
    assertEquals(EXPECTED_CLOSING, subject.earliestDepartureTime(EXPECTED_CLOSING));
  }

  @Test
  void earliestDepartureTimeIsAfterClosing() {
    var subject = new AccessWithPenalty(
      walk(STOP, DURATION).openingHours(OPEN, CLOSE).withTimePenalty(PENALTY)
    );

    // If time is after closing, then wait until it opens next day. This is TestAccessEgress
    // implementation specific.
    assertEquals(EXPECTED_OPENING + ONE_DAY, subject.earliestDepartureTime(EXPECTED_CLOSING + 1));
  }

  @Test
  void earliestDepartureTimeWhenServiceIsClosed() {
    // Test closed
    var subject = new AccessWithPenalty(
      walk(STOP, DURATION).openingHoursClosed().withTimePenalty(PENALTY)
    );
    assertEquals(TIME_NOT_SET, subject.earliestDepartureTime(ANY_TIME));
  }

  @Test
  void removeDecoratorIfItExist() {
    var original = walk(STOP, DURATION).withTimePenalty(PENALTY);
    var subject = new AccessWithPenalty(original);
    assertSame(original, subject.removeDecorator());
  }
}
