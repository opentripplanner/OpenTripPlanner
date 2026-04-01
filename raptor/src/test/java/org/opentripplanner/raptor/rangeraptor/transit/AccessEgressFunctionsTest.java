package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByRound;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByStop;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForMcRaptor;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNonOptimalPathsForStandardRaptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

class AccessEgressFunctionsTest implements RaptorTestConstants {

  public static final int BOARD_SLACK = D20_s;
  public static final int ALIGHT_SLACK = D10_s;
  public static final int TRANSFER_SLACK = D1_m;

  private static final int STOP = 8;
  private static final int C1 = 1000;
  private static final int C1_LOW = 999;

  private static final RaptorAccessEgress WALK_10_m = TestAccessEgress.walk(STOP, D10_m, C1);
  private static final RaptorAccessEgress WALK_10_m_C1_LOW = TestAccessEgress.walk(
    STOP,
    D10_m,
    C1_LOW
  );
  private static final RaptorAccessEgress WALK_8_m = TestAccessEgress.walk(STOP, D8_m, C1);

  private static final RaptorAccessEgress WALK_10_m_VIA_1 = TestAccessEgress.walk(
    STOP,
    D10_m,
    C1
  ).withViaLocationsVisited(1);

  private static final RaptorAccessEgress FLEX_1x_10_m = flex(STOP, D10_m, 1, C1);
  private static final RaptorAccessEgress FLEX_1x_8_m = flex(STOP, D8_m, 1, C1);
  private static final RaptorAccessEgress FLEX_2x_8_m = flex(STOP, D8_m, 2, C1);
  private static final RaptorAccessEgress FLEX_AND_WALK_1x_8_m = flexAndWalk(STOP, D8_m, 1, C1);
  private static final RaptorAccessEgress WALK_W_OPENING_HOURS_8_m = TestAccessEgress.walk(
    STOP,
    D8_m,
    C1
  ).openingHours(T00_00, T01_00);

  private static final RaptorAccessEgress WALK_W_OPENING_HOURS_8_m_OTHER = TestAccessEgress.walk(
    STOP,
    D8_m,
    C1
  ).openingHours(T00_10, T01_00);

  @Test
  void removeNonOptimalPathsForStandardRaptorTest() {
    // Empty set
    assertElements(List.of(), removeNonOptimalPathsForStandardRaptor(List.of()));

    // One element
    assertElements(List.of(WALK_8_m), removeNonOptimalPathsForStandardRaptor(List.of(WALK_8_m)));

    // Shortest duration
    assertElements(
      List.of(WALK_8_m),
      removeNonOptimalPathsForStandardRaptor(List.of(WALK_8_m, WALK_10_m))
    );

    // Fewest rides
    assertElements(
      List.of(FLEX_1x_8_m),
      removeNonOptimalPathsForStandardRaptor(List.of(FLEX_1x_8_m, FLEX_2x_8_m))
    );

    // Number of via locations visited, higher is better
    assertElements(
      List.of(WALK_10_m_VIA_1, WALK_8_m),
      removeNonOptimalPathsForStandardRaptor(List.of(WALK_10_m, WALK_10_m_VIA_1, WALK_8_m))
    );

    // Arriving at the stop on-board, and by-foot.
    // OnBoard is better because we can do a transfer walk to nearby stops.
    assertElements(
      List.of(FLEX_1x_8_m),
      removeNonOptimalPathsForStandardRaptor(List.of(FLEX_AND_WALK_1x_8_m, FLEX_1x_8_m))
    );

    // Flex+walk is faster, flex arrive on-board, both is optimal
    assertElements(
      List.of(FLEX_AND_WALK_1x_8_m, FLEX_1x_10_m),
      removeNonOptimalPathsForStandardRaptor(List.of(FLEX_AND_WALK_1x_8_m, FLEX_1x_10_m))
    );

    // Walk has few rides, and Flex is faster - both is optimal
    assertElements(
      List.of(WALK_10_m, FLEX_1x_8_m),
      removeNonOptimalPathsForStandardRaptor(List.of(WALK_10_m, FLEX_1x_8_m))
    );

    // Walk without opening hours is better than with, because it can be time-shifted without
    // any constraints
    assertElements(
      List.of(WALK_8_m),
      removeNonOptimalPathsForStandardRaptor(List.of(WALK_8_m, WALK_W_OPENING_HOURS_8_m))
    );

    // Walk with opening hours can NOT dominate another access/egress without - even if it is
    // faster. The reason is that it may not be allowed to time-shift it to the desired time.
    assertElements(
      List.of(WALK_10_m, WALK_W_OPENING_HOURS_8_m),
      removeNonOptimalPathsForStandardRaptor(List.of(WALK_10_m, WALK_W_OPENING_HOURS_8_m))
    );

    // If two paths both have opening hours, both should be accepted.
    assertElements(
      List.of(WALK_W_OPENING_HOURS_8_m, WALK_W_OPENING_HOURS_8_m_OTHER),
      removeNonOptimalPathsForStandardRaptor(
        List.of(WALK_W_OPENING_HOURS_8_m, WALK_W_OPENING_HOURS_8_m_OTHER)
      )
    );
  }

  @Test
  void removeNonOptimalPathsForMcRaptorTest() {
    // Empty set
    assertElements(List.of(), removeNonOptimalPathsForMcRaptor(List.of()));

    // One element
    assertElements(List.of(WALK_8_m), removeNonOptimalPathsForMcRaptor(List.of(WALK_8_m)));

    // Lowest cost
    assertElements(
      List.of(WALK_10_m_C1_LOW),
      removeNonOptimalPathsForMcRaptor(List.of(WALK_10_m, WALK_10_m_C1_LOW))
    );

    // Shortest duration
    assertElements(
      List.of(WALK_8_m),
      removeNonOptimalPathsForMcRaptor(List.of(WALK_8_m, WALK_10_m))
    );

    // Fewest rides
    assertElements(
      List.of(FLEX_1x_8_m),
      removeNonOptimalPathsForMcRaptor(List.of(FLEX_1x_8_m, FLEX_2x_8_m))
    );

    // Arriving at the stop on-board, and by-foot.
    // OnBoard is better because we can do a transfer walk to nearby stops.
    assertElements(
      List.of(FLEX_1x_8_m),
      removeNonOptimalPathsForMcRaptor(List.of(FLEX_AND_WALK_1x_8_m, FLEX_1x_8_m))
    );

    // Flex+walk is faster, flex arrive on-board, both is optimal
    assertElements(
      List.of(FLEX_AND_WALK_1x_8_m, FLEX_1x_10_m),
      removeNonOptimalPathsForStandardRaptor(List.of(FLEX_AND_WALK_1x_8_m, FLEX_1x_10_m))
    );

    // Walk has few rides, and Flex is faster - both is optimal
    assertElements(
      List.of(WALK_10_m, FLEX_1x_8_m),
      removeNonOptimalPathsForMcRaptor(List.of(WALK_10_m, FLEX_1x_8_m))
    );

    // Walk without opening hours is better than with, because it can be time-shifted without
    // any constraints
    assertElements(
      List.of(WALK_8_m),
      removeNonOptimalPathsForMcRaptor(List.of(WALK_8_m, WALK_W_OPENING_HOURS_8_m))
    );

    // Walk with opening hours can NOT dominate another access/egress without - even if it is
    // faster. The reason is that it may not be allowed to time-shift it to the desired time.
    assertElements(
      List.of(WALK_10_m, WALK_W_OPENING_HOURS_8_m),
      removeNonOptimalPathsForMcRaptor(List.of(WALK_10_m, WALK_W_OPENING_HOURS_8_m))
    );

    // If two paths both have opening hours, both should be accepted.
    assertElements(
      List.of(WALK_W_OPENING_HOURS_8_m, WALK_W_OPENING_HOURS_8_m_OTHER),
      removeNonOptimalPathsForMcRaptor(
        List.of(WALK_W_OPENING_HOURS_8_m, WALK_W_OPENING_HOURS_8_m_OTHER)
      )
    );
  }

  @Test
  void groupByRoundTest() {
    // Map one element
    var res = groupByRound(List.of(WALK_8_m), e -> true);
    assertArrayEquals(new int[] { 0 }, res.keys());
    assertElements(List.of(WALK_8_m), res.get(0));

    // Map 4 elements into 3 groups
    res = groupByRound(List.of(WALK_8_m, FLEX_1x_8_m, FLEX_2x_8_m, FLEX_1x_10_m), e -> true);
    int[] keys = res.keys();
    Arrays.sort(keys);

    assertArrayEquals(new int[] { 0, 1, 2 }, keys);
    assertElements(List.of(WALK_8_m), res.get(0));
    assertElements(List.of(FLEX_1x_8_m, FLEX_1x_10_m), res.get(1));
    assertElements(List.of(FLEX_2x_8_m), res.get(2));

    // Apply same test, but remove entries with number of rides == 1
    res = groupByRound(List.of(WALK_8_m, FLEX_1x_8_m), RaptorAccessEgress::hasRides);
    keys = res.keys();
    Arrays.sort(keys);

    assertArrayEquals(new int[] { 1 }, keys);
    assertElements(List.of(FLEX_1x_8_m), res.get(1));
  }

  @Test
  void groupByStopTest() {
    // Map one element
    var res = groupByStop(List.of(WALK_8_m));
    assertArrayEquals(new int[] { STOP }, res.keys());
    assertElements(List.of(WALK_8_m), res.get(STOP));

    // Map 4 elements into 3 groups
    var walk_99 = TestAccessEgress.walk(99, D1_s);
    res = groupByStop(List.of(WALK_8_m, WALK_10_m, walk_99));
    int[] keys = res.keys();
    Arrays.sort(keys);

    assertArrayEquals(new int[] { STOP, 99 }, keys);
    assertElements(List.of(WALK_8_m, WALK_10_m), res.get(STOP));
    assertElements(List.of(walk_99), res.get(99));
  }

  private static <T> void assertElements(Collection<T> expected, Collection<T> result) {
    assertEquals(
      expected.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")),
      result.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
    );
  }
}
