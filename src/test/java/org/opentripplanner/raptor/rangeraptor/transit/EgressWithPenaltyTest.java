package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.raptor._data.RaptorTestConstants.SECONDS_IN_A_DAY;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import org.junit.jupiter.api.Test;

class EgressWithPenaltyTest {

  private static final int STOP = 17;
  private static final int DURATION = 400;
  private static final int PENALTY = 100;
  private static final int OPEN = 600;
  private static final int CLOSE = 1800;
  private static final int ANY_TIME = 500;

  // We are interested in the opening hours at the arrival, not in the beginning of the egress leg.
  // Hence, we must add egress duration as well here.
  private static final int EXPECTED_OPENING = OPEN + PENALTY + DURATION;
  private static final int EXPECTED_CLOSING = CLOSE + PENALTY + DURATION;
  private static final int ONE_DAY = SECONDS_IN_A_DAY;

  @Test
  void durationInSeconds() {
    var subject = new EgressWithPenalty(walk(STOP, DURATION).withTimePenalty(PENALTY));
    assertEquals(DURATION + PENALTY, subject.durationInSeconds());
  }

  @Test
  void latestArrivalTimeIsBeforeOpeningHours() {
    var subject = new EgressWithPenalty(
      walk(STOP, DURATION).openingHours(OPEN, CLOSE).withTimePenalty(PENALTY)
    );

    // If time is before opening, then time-shift to the closing of the previous day. This is
    // TestAccessEgress implementation specific.
    assertEquals(EXPECTED_CLOSING - ONE_DAY, subject.latestArrivalTime(EXPECTED_OPENING - 1));
  }

  @Test
  void latestArrivalTimeIsInsideOpeningHours() {
    var subject = new EgressWithPenalty(
      walk(STOP, DURATION).openingHours(OPEN, CLOSE).withTimePenalty(PENALTY)
    );

    assertEquals(EXPECTED_OPENING, subject.latestArrivalTime(EXPECTED_OPENING));
    assertEquals(EXPECTED_CLOSING, subject.latestArrivalTime(EXPECTED_CLOSING));
  }

  @Test
  void latestArrivalTimeIsAfterClosing() {
    var subject = new EgressWithPenalty(
      walk(STOP, DURATION).openingHours(OPEN, CLOSE).withTimePenalty(PENALTY)
    );

    assertEquals(EXPECTED_CLOSING, subject.latestArrivalTime(EXPECTED_CLOSING + 1));
    assertEquals(EXPECTED_CLOSING, subject.latestArrivalTime(EXPECTED_CLOSING + 100));
  }

  @Test
  void latestArrivalTimeWhenServiceIsClosed() {
    // Test closed
    var subject = new EgressWithPenalty(
      walk(STOP, DURATION).openingHoursClosed().withTimePenalty(PENALTY)
    );
    assertEquals(TIME_NOT_SET, subject.latestArrivalTime(ANY_TIME));
  }

  @Test
  void removeDecoratorIfItExist() {
    var original = walk(STOP, DURATION).withTimePenalty(PENALTY);
    var subject = new EgressWithPenalty(original);
    assertSame(original, subject.removeDecorator());
  }
}
