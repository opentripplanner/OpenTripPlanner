package org.opentripplanner.transit.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CriteriaCollectionTest {

  @Test
  void testEmptyIncludeEverything() {
    CriteriaCollection<String> criteriaCollection = CriteriaCollection.ofEmptyIsEverything(
      List.of()
    );
    assertTrue(criteriaCollection.includeEverything());
  }

  @Test
  void testNullIncludeEverything() {
    CriteriaCollection<String> nullCollection = null;
    CriteriaCollection<String> criteriaCollection = CriteriaCollection.ofEmptyIsEverything(
      nullCollection
    );
    assertTrue(criteriaCollection.includeEverything());

    List<String> nullList = null;
    CriteriaCollection<String> criteriaCollection2 = CriteriaCollection.ofEmptyIsEverything(
      nullList
    );
    assertTrue(criteriaCollection2.includeEverything());
  }
}
