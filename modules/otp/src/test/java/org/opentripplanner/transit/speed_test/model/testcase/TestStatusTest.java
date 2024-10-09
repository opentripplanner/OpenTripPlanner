package org.opentripplanner.transit.speed_test.model.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.speed_test.model.testcase.TestStatus.FAILED;
import static org.opentripplanner.transit.speed_test.model.testcase.TestStatus.NA;
import static org.opentripplanner.transit.speed_test.model.testcase.TestStatus.OK;
import static org.opentripplanner.transit.speed_test.model.testcase.TestStatus.WARN;

import org.junit.jupiter.api.Test;

class TestStatusTest {

  @Test
  void ok() {
    assertTrue(OK.ok());
    assertFalse(WARN.ok());
  }

  @Test
  void notOk() {
    assertFalse(OK.notOk());
    assertTrue(NA.notOk());
  }

  @Test
  void failed() {
    assertTrue(FAILED.failed());
    assertFalse(OK.failed());
  }

  @Test
  void highestSeverity() {
    // verify order: OK -> NA -> WARN -> FAILED
    assertEquals(OK, OK.highestSeverity(OK));
    assertEquals(NA, OK.highestSeverity(NA));
    assertEquals(NA, NA.highestSeverity(OK));
    assertEquals(WARN, NA.highestSeverity(WARN));
    assertEquals(WARN, WARN.highestSeverity(NA));
    assertEquals(FAILED, WARN.highestSeverity(FAILED));
    assertEquals(FAILED, FAILED.highestSeverity(WARN));
    assertEquals(FAILED, FAILED.highestSeverity(FAILED));
  }
}
