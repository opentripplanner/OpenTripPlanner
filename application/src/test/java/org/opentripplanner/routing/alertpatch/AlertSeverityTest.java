package org.opentripplanner.routing.alertpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AlertSeverityTest {

  static Stream<Arguments> sortingIndexCases() {
    return Stream.of(
      Arguments.of(AlertSeverity.INFO, 0),
      Arguments.of(AlertSeverity.UNKNOWN_SEVERITY, 1),
      Arguments.of(AlertSeverity.VERY_SLIGHT, 2),
      Arguments.of(AlertSeverity.SLIGHT, 3),
      Arguments.of(AlertSeverity.WARNING, 4),
      Arguments.of(AlertSeverity.SEVERE, 5),
      Arguments.of(AlertSeverity.VERY_SEVERE, 6)
    );
  }

  @ParameterizedTest
  @MethodSource("sortingIndexCases")
  void sortingIndexShouldMatchExpectedOrder(AlertSeverity severity, int expectedSortingIndex) {
    assertEquals(expectedSortingIndex, severity.sortingIndex());
  }
}
