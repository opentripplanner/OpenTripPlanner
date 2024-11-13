package org.opentripplanner.transit.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CriteriaCollectionTest {

  @Test
  void testEmptyMatchesAll() {
    CriteriaCollection<String> criteriaCollection = CriteriaCollection.of(List.of());
    assertTrue(criteriaCollection.matchesAll());
  }

  @Test
  void testNullMatchesAll() {
    CriteriaCollection<String> nullCollection = null;
    CriteriaCollection<String> criteriaCollection = CriteriaCollection.of(nullCollection);
    assertTrue(criteriaCollection.matchesAll());

    List<String> nullList = null;
    CriteriaCollection<String> criteriaCollection2 = CriteriaCollection.of(nullList);
    assertTrue(criteriaCollection2.matchesAll());
  }

}