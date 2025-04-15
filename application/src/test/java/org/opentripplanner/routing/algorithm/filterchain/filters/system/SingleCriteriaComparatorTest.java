package org.opentripplanner.routing.algorithm.filterchain.filters.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.grouppriority.DefaultTransitGroupPriorityCalculator;

class SingleCriteriaComparatorTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final DefaultTransitGroupPriorityCalculator GROUP_PRIORITY_CALCULATOR =
    new DefaultTransitGroupPriorityCalculator();

  private static final Place A = TEST_MODEL.place("A", 10, 11);
  private static final Place B = TEST_MODEL.place("B", 10, 13);
  private static final Place C = TEST_MODEL.place("C", 10, 14);
  private static final Place D = TEST_MODEL.place("D", 10, 15);

  private static final int START = 1000;
  private static final int TX_AT = 1500;
  private static final int END_LOW = 2000;

  // [Tx, Cost] => [0, 1240]
  private static final Itinerary zeroTransferLowCost = newItinerary(A)
    .bus(1, START, END_LOW, B)
    .walk(60, C)
    .build();
  // [Tx, Cost] => [0, 1360]
  private static final Itinerary zeroTransferHighCost = newItinerary(A)
    .bus(1, START, END_LOW, B)
    .walk(120, C)
    .build();
  // [Tx, Cost] => [1, 1240]
  private static final Itinerary oneTransferLowCost = newItinerary(A)
    .bus(1, START, TX_AT, B)
    .bus(2, TX_AT, END_LOW, C)
    .build();

  @BeforeAll
  static void setUp() {
    assertEquals(0, zeroTransferLowCost.numberOfTransfers());
    assertEquals(0, zeroTransferHighCost.numberOfTransfers());
    assertEquals(1, oneTransferLowCost.numberOfTransfers());

    int expectedCost = zeroTransferLowCost.generalizedCost();
    assertTrue(expectedCost < zeroTransferHighCost.generalizedCost());
    assertEquals(expectedCost, oneTransferLowCost.generalizedCost());
  }

  @Test
  void strictOrder() {
    assertTrue(SingleCriteriaComparator.compareNumTransfers().strictOrder());
    assertTrue(SingleCriteriaComparator.compareGeneralizedCost().strictOrder());
    assertFalse(SingleCriteriaComparator.compareTransitGroupsPriority().strictOrder());
  }

  @Test
  void compareNumTransfers() {
    var subject = SingleCriteriaComparator.compareNumTransfers();

    // leftDominanceExist
    assertFalse(subject.leftDominanceExist(zeroTransferHighCost, zeroTransferLowCost));
    assertTrue(subject.leftDominanceExist(zeroTransferLowCost, oneTransferLowCost));
    assertFalse(subject.leftDominanceExist(oneTransferLowCost, zeroTransferLowCost));

    // strict order expected
    assertTrue(subject.strictOrder());
  }

  @Test
  void compareGeneralizedCost() {
    var subject = SingleCriteriaComparator.compareGeneralizedCost();

    // leftDominanceExist
    assertFalse(subject.leftDominanceExist(zeroTransferHighCost, zeroTransferLowCost));
    assertTrue(subject.leftDominanceExist(zeroTransferLowCost, zeroTransferHighCost));
    assertFalse(subject.leftDominanceExist(zeroTransferLowCost, oneTransferLowCost));

    // strict order expected
    assertTrue(subject.strictOrder());
  }

  @Test
  void compareTransitPriorityGroups() {
    var group1 = newItinerary(A)
      .bus(1, START, END_LOW, C)
      .itineraryBuilder()
      .withGeneralizedCost2(1)
      .build();
    var group2 = newItinerary(A)
      .bus(1, START, END_LOW, C)
      .itineraryBuilder()
      .withGeneralizedCost2(2)
      .build();
    var group1And2 = newItinerary(A)
      .bus(1, START, END_LOW, C)
      .itineraryBuilder()
      .withGeneralizedCost2(GROUP_PRIORITY_CALCULATOR.mergeInGroupId(1, 2))
      .build();

    var subject = SingleCriteriaComparator.compareTransitGroupsPriority();

    assertTrue(subject.leftDominanceExist(group1, group2));
    assertTrue(subject.leftDominanceExist(group2, group1));
    assertTrue(subject.leftDominanceExist(group1, group1And2));
    assertTrue(subject.leftDominanceExist(group2, group1And2));
    assertFalse(subject.leftDominanceExist(group1And2, group1));
    assertFalse(subject.leftDominanceExist(group1And2, group1));

    // Cannot be ordered => compare will fail
    assertFalse(subject.strictOrder());
  }
}
