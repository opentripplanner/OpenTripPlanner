package org.opentripplanner.routing.alertpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Test;

class AlertSeverityTest {

  @Test
  void sortingIndexShouldMatchOrdinalForAllValues() {
    for (var severity : AlertSeverity.values()) {
      assertEquals(severity.ordinal(), severity.sortingIndex());
    }
  }

  @Test
  void sortingBySortingIndexShouldFollowEnumDeclarationOrder() {
    var sorted = Arrays.stream(AlertSeverity.values())
      .sorted(Comparator.comparingInt(AlertSeverity::sortingIndex))
      .toList();

    assertEquals(Arrays.asList(AlertSeverity.values()), sorted);
  }
}
