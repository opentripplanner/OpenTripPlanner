package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class StopTimeKeyTest {

  private static final String ID = "1";
  private static final int STOP_SEQUENCE_NUMBER = 1;

  private static final StopTimeKey SUBJECT = StopTimeKey.of(
    TimetableRepositoryForTest.id(ID),
    STOP_SEQUENCE_NUMBER
  ).build();

  @Test
  void copy() {
    assertEquals(ID + "_#" + STOP_SEQUENCE_NUMBER, SUBJECT.getId().getId());

    // Make a copy
    var copy = SUBJECT.copy().build();

    assertEquals(ID + "_#" + STOP_SEQUENCE_NUMBER, copy.getId().getId());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
  }
}
