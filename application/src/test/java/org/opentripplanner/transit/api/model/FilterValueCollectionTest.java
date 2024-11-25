package org.opentripplanner.transit.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FilterValueCollectionTest {

  @Test
  void testEmptyIncludeEverything() {
    FilterValueCollection<String> filterValueCollection = FilterValueCollection.ofEmptyIsEverything(
      List.of()
    );
    assertTrue(filterValueCollection.includeEverything());
  }

  @Test
  void testNullIncludeEverything() {
    List<String> nullList = null;
    FilterValueCollection<String> filterValueCollection2 = FilterValueCollection.ofEmptyIsEverything(
      nullList
    );
    assertTrue(filterValueCollection2.includeEverything());
  }
}
