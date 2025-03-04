package org.opentripplanner.routing.algorithm.filterchain.filters.system.mcmax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.SingleCriteriaComparator;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.grouppriority.DefaultTransitGroupPriorityCalculator;

class McMaxLimitFilterTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final DefaultTransitGroupPriorityCalculator GROUP_PRIORITY_CALCULATOR =
    new DefaultTransitGroupPriorityCalculator();

  private static final Place A = TEST_MODEL.place("A", 10, 11);
  private static final Place B = TEST_MODEL.place("B", 10, 13);
  private static final Place C = TEST_MODEL.place("C", 10, 14);
  private static final Place D = TEST_MODEL.place("D", 10, 15);
  private static final Place E = TEST_MODEL.place("E", 10, 15);
  private static final Place[] PLACES = { A, B, C, D, E };

  private static final int START = 3600 * 10;

  // Note! Each group id needs to be a power of 2. This is implementation-specific, but using the
  //       TransitGroupPriorityService here to generate these ids is a bit over-kill.
  private static final int GROUP_A = 0x01;
  private static final int GROUP_B = 0x02;
  private static final int GROUP_C = 0x04;
  private static final int GROUP_AB = GROUP_PRIORITY_CALCULATOR.mergeInGroupId(GROUP_A, GROUP_B);
  private static final int GROUP_BC = GROUP_PRIORITY_CALCULATOR.mergeInGroupId(GROUP_B, GROUP_C);
  private static final int GROUP_ABC = GROUP_PRIORITY_CALCULATOR.mergeInGroupId(GROUP_AB, GROUP_C);

  private static final boolean EXP_KEEP = true;
  private static final boolean EXP_DROP = false;

  private static final int COST_LOW = 1000;
  private static final int COST_MED = 1200;
  private static final int COST_HIGH = 1500;

  private static final int TX_0 = 0;
  private static final int TX_1 = 1;
  private static final int TX_2 = 2;

  private final McMaxLimitFilter subject = new McMaxLimitFilter(
    "test",
    2,
    List.of(
      SingleCriteriaComparator.compareGeneralizedCost(),
      SingleCriteriaComparator.compareNumTransfers(),
      SingleCriteriaComparator.compareTransitGroupsPriority()
    )
  );

  static TestRow row(
    boolean expected,
    int c1,
    int nTransfers,
    int transitGroups,
    String description
  ) {
    return new TestRow(expected, c1, nTransfers, transitGroups);
  }

  static List<List<TestRow>> filterTestCases() {
    return List.of(
      List.of(/* Should not fail for an empty list of itineraries*/),
      List.of(
        // Test minNumItinerariesLimit = 2
        row(EXP_KEEP, COST_LOW, TX_1, GROUP_A, "Best in everything"),
        row(EXP_KEEP, COST_HIGH, TX_2, GROUP_AB, "Worse, kept because minNumItinerariesLimit is 2")
      ),
      List.of(
        // Test minNumItinerariesLimit, first is added
        row(EXP_KEEP, COST_HIGH, TX_2, GROUP_ABC, "Worst, kept because of minNumItinerariesLimit"),
        row(EXP_KEEP, COST_LOW, TX_0, GROUP_A, "Best in everything"),
        row(EXP_DROP, COST_HIGH, TX_1, GROUP_AB, "Dropped because not better than #2.")
      ),
      List.of(
        // The minNumItinerariesLimit is met, so no extra itinerary(#0) is added
        row(EXP_DROP, COST_HIGH, TX_2, GROUP_AB, "First element is dropped"),
        row(EXP_KEEP, COST_LOW, TX_1, GROUP_B, "Best cost and group B"),
        row(EXP_KEEP, COST_MED, TX_0, GROUP_A, "Best nTransfers and group A")
      ),
      List.of(
        row(EXP_KEEP, COST_LOW, TX_2, GROUP_A, "Best: c1 and group A"),
        row(EXP_DROP, COST_LOW, TX_1, GROUP_AB, "Best compromise: c1, Tx, and group AB"),
        row(EXP_KEEP, COST_LOW, TX_2, GROUP_C, "Best: c1 and group C"),
        row(EXP_KEEP, COST_MED, TX_0, GROUP_BC, "Best: num-of-transfers")
      ),
      /**
       * This is the example explained in JavaDoc {@link McMaxLimitFilter}
       */
      List.of(
        row(EXP_DROP, COST_LOW, TX_1, GROUP_A, ""),
        row(EXP_DROP, COST_LOW, TX_2, GROUP_AB, ""),
        row(EXP_KEEP, COST_LOW, TX_2, GROUP_B, "Kept -> Only one in group B"),
        row(EXP_DROP, COST_MED, TX_0, GROUP_AB, ""),
        row(EXP_KEEP, COST_MED, TX_0, GROUP_A, "Kept -> Best transfer and group A"),
        row(EXP_KEEP, COST_HIGH, TX_1, GROUP_C, "Kept -> Best group C, tie with #6"),
        row(EXP_DROP, COST_HIGH, TX_2, GROUP_C, "")
      )
    );
  }

  @ParameterizedTest
  @MethodSource("filterTestCases")
  void filterTest(List<TestRow> rows) {
    var input = rows.stream().map(TestRow::create).toList();
    var expected = rows.stream().filter(TestRow::expected).map(TestRow::create).toList();

    var result = subject.removeMatchesForTest(input);

    assertEquals(toStr(expected), toStr(result));
  }

  @Test
  void testName() {
    assertEquals("test", subject.name());
  }

  /**
   * Make sure the test setup is correct - this does not test anything in src/main
   */
  @Test
  void testGroupsToString() {
    assertEquals("A", groupsToString(GROUP_A));
    assertEquals("B", groupsToString(GROUP_B));
    assertEquals("C", groupsToString(GROUP_C));
    assertEquals("AB", groupsToString(GROUP_AB));
    assertEquals("BC", groupsToString(GROUP_BC));
    assertEquals("ABC", groupsToString(GROUP_ABC));
  }

  private static String groupsToString(int groups) {
    var buf = new StringBuilder();
    char ch = 'A';
    // Check for 5 groups - the test does not use so many, but it does not matter
    for (int i = 0; i < 5; ++i) {
      int mask = 1 << i;
      if ((groups & mask) != 0) {
        buf.append(ch);
      }
      ch = (char) (ch + 1);
    }
    return buf.toString();
  }

  private static String toStr(List<Itinerary> list) {
    return list
      .stream()
      .map(i ->
        "[ %d %d %s ]".formatted(
            i.getGeneralizedCost(),
            i.getNumberOfTransfers(),
            groupsToString(i.getGeneralizedCost2().orElse(-1))
          )
      )
      .collect(Collectors.joining(", "));
  }

  record TestRow(boolean expected, int c1, int nTransfers, int transitGroupIds) {
    Itinerary create() {
      int start = START;
      var builder = newItinerary(A);

      if (nTransfers < 0) {
        builder.drive(start, ++start, E);
      } else {
        builder.bus(1, ++start, ++start, PLACES[1]);
        for (int i = 0; i < nTransfers; i++) {
          builder.bus(1, ++start, ++start, PLACES[i + 2]);
        }
        builder.withGeneralizedCost2(transitGroupIds);
      }
      return builder.build(c1);
    }

    @Override
    public String toString() {
      // The red-x is a unicode character(U+274C) and should be visible in most IDEs.
      return "%s %d %d %s".formatted(
          expected ? "" : "❌",
          c1,
          nTransfers,
          groupsToString(transitGroupIds)
        );
    }
  }
}
