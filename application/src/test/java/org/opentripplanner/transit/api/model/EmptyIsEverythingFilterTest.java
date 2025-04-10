package org.opentripplanner.transit.api.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class EmptyIsEverythingFilterTest {

  @Test
  void testEmptyIncludeEverything() {
    FilterValues<String> filterValues = FilterValues.ofEmptyIsEverything("null", List.of());
    assertTrue(filterValues.includeEverything());
  }

  @Test
  void testNullIncludeEverything() {
    List<String> nullList = null;
    FilterValues<String> filterValues2 = FilterValues.ofEmptyIsEverything("null", nullList);
    assertTrue(filterValues2.includeEverything());
  }
}
