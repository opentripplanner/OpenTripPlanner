package org.opentripplanner.routing.alertpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AlertSeverityTest {

  @ParameterizedTest
  @CsvSource(
    {
      "INFO,0",
      "UNDEFINED,1",
      "UNKNOWN_SEVERITY,2",
      "VERY_SLIGHT,3",
      "SLIGHT,4",
      "WARNING,5",
      "SEVERE,6",
      "VERY_SEVERE,7",
    }
  )
  void sortingIndexShouldMatchExpectedOrder(AlertSeverity severity, int expectedSortingIndex) {
    assertEquals(expectedSortingIndex, severity.sortingIndex());
  }
}
