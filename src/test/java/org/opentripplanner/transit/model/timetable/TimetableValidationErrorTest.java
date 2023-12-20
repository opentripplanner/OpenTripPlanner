package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;

class TimetableValidationErrorTest {

  private TimetableValidationError subject = new TimetableValidationError(
    TimetableValidationError.ErrorCode.NEGATIVE_HOP_TIME,
    3,
    TransitModelForTest.trip("A").withMode(TransitMode.BUS).withShortName("Line A").build()
  );

  @Test
  void message() {
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 3 in trip Trip{F:A Line A}.",
      subject.message()
    );
  }
}
