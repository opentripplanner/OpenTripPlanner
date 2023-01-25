package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.calculateEgressDepartureTime;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByRound;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.groupByStop;
import static org.opentripplanner.raptor.rangeraptor.transit.AccessEgressFunctions.removeNoneOptimalPathsForStandardRaptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleSubscriptions;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;

class AccessEgressFunctionsTest implements RaptorTestConstants {

  public static final int T00_31 = hm2time(0, 31);
  public static final int BOARD_SLACK = D20s;
  public static final int ALIGHT_SLACK = D10s;
  public static final int TRANSFER_SLACK = D1m;
  public static final RaptorSlackProvider EXTERNAL_SLACK_PROVIDER = new DefaultSlackProvider(
    TRANSFER_SLACK,
    BOARD_SLACK,
    ALIGHT_SLACK
  );

  private static final int STOP = 8;

  private static final RaptorAccessEgress WALK_10m = TestAccessEgress.walk(STOP, D10m);
  private static final RaptorAccessEgress WALK_8m = TestAccessEgress.walk(STOP, D8m);
  private static final RaptorAccessEgress FLEX_1x_10m = flex(STOP, D10m, 1);
  private static final RaptorAccessEgress FLEX_1x_8m = flex(STOP, D8m, 1);
  private static final RaptorAccessEgress FLEX_2x_8m = flex(STOP, D8m, 2);
  private static final RaptorAccessEgress FLEX_AND_WALK_1x_8m = flexAndWalk(STOP, D8m, 1);
  private static final RaptorAccessEgress WALK_W_OPENING_HOURS_8m = TestAccessEgress
    .walk(STOP, D8m)
    .openingHours(T00_00, T01_00);

  @Test
  void calculateEgressDepartureTimeForwardSearch() {
    var slackProvider = SlackProviderAdapter.forwardSlackProvider(
      EXTERNAL_SLACK_PROVIDER,
      new LifeCycleSubscriptions()
    );
    var calculator = new ForwardTimeCalculator();

    // No time-shift expected for a regular walking egress
    assertEquals(T00_30, calculateEgressDepartureTime(T00_30, WALK_8m, slackProvider, calculator));
    // Transfers slack should be added if the egress arrive on-board
    assertEquals(
      T00_30 + TRANSFER_SLACK,
      calculateEgressDepartureTime(T00_30, FLEX_1x_8m, slackProvider, calculator)
    );
    // Transfers slack should be added if the flex egress arrive by walking
    assertEquals(
      T00_30 + TRANSFER_SLACK,
      calculateEgressDepartureTime(T00_30, FLEX_AND_WALK_1x_8m, slackProvider, calculator)
    );
    // No time-shift expected if egress is within opening hours
    assertEquals(
      T00_30,
      calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.walk(STOP, D8m).openingHours(T00_00, T01_00),
        slackProvider,
        calculator
      )
    );
    // Egress should be time-shifted to the opening hours if departure time is before
    assertEquals(
      T00_30,
      calculateEgressDepartureTime(
        T00_10,
        TestAccessEgress.walk(STOP, D8m).openingHours(T00_30, T01_00),
        slackProvider,
        calculator
      )
    );
    // Egress should be time-shifted to the next opening hours if departure time is after
    // opening hours
    assertEquals(
      T00_10 + D24h,
      calculateEgressDepartureTime(
        T00_31,
        TestAccessEgress.walk(STOP, D8m).openingHours(T00_10, T00_30),
        slackProvider,
        calculator
      )
    );

    // If egress is are closed (opening hours) then -1 should be returned
    assertEquals(
      SearchParams.TIME_NOT_SET,
      calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.walk(STOP, D8m).openingHours(5, 4),
        slackProvider,
        calculator
      )
    );
  }

  @Test
  void calculateEgressDepartureTimeReverseSearch() {
    var slackProvider = SlackProviderAdapter.forwardSlackProvider(
      EXTERNAL_SLACK_PROVIDER,
      new LifeCycleSubscriptions()
    );
    var calculator = new ReverseTimeCalculator();

    // No time-shift expected for a regular walking egress
    assertEquals(T00_30, calculateEgressDepartureTime(T00_30, WALK_8m, slackProvider, calculator));
    // Transfers slack should be subtracted(reverse search) if the egress arrive on-board
    assertEquals(
      T00_30 - TRANSFER_SLACK,
      calculateEgressDepartureTime(T00_30, FLEX_1x_8m, slackProvider, calculator)
    );
    // Transfers slack should be subtracted(reverse search) if the flex egress arrive by walking
    assertEquals(
      T00_30 - TRANSFER_SLACK,
      calculateEgressDepartureTime(T00_30, FLEX_AND_WALK_1x_8m, slackProvider, calculator)
    );
    // No time-shift expected if egress is within opening hours
    assertEquals(
      T00_30,
      calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.walk(STOP, D8m).openingHours(T00_00, T01_00),
        slackProvider,
        calculator
      )
    );
    // Egress should be time-shifted to the closing opening hours (entrance) plus the duration
    // of the egress (to get to the exit) if the departure time is after.
    assertEquals(
      T00_30 + D5m,
      calculateEgressDepartureTime(
        T00_40,
        TestAccessEgress.walk(STOP, D5m).openingHours(T00_10, T00_30),
        slackProvider,
        calculator
      )
    );
    // Egress should be time-shifted to the next opening hours if departure time is after
    // opening hours
    assertEquals(
      T00_30 + D3m - D24h,
      calculateEgressDepartureTime(
        T00_00,
        TestAccessEgress.walk(STOP, D3m).openingHours(T00_10, T00_30),
        slackProvider,
        calculator
      )
    );

    // If egress is are closed (opening hours) then -1 should be returned
    assertEquals(
      -1,
      calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.walk(STOP, D8m).openingHours(5, 4),
        slackProvider,
        calculator
      )
    );
  }

  @Test
  void removeNoneOptimalPathsForStandardRaptorTest() {
    // Empty set
    assertElements(List.of(), removeNoneOptimalPathsForStandardRaptor(List.of()));

    // One element
    assertElements(List.of(WALK_8m), removeNoneOptimalPathsForStandardRaptor(List.of(WALK_8m)));

    // Shortest duration
    assertElements(
      List.of(WALK_8m),
      removeNoneOptimalPathsForStandardRaptor(List.of(WALK_8m, WALK_10m))
    );

    // Fewest rides
    assertElements(
      List.of(FLEX_1x_8m),
      removeNoneOptimalPathsForStandardRaptor(List.of(FLEX_1x_8m, FLEX_2x_8m))
    );

    // Arriving at the stop on-board, and by-foot.
    // This is better because we can do a transfer walk to nearby stops.
    assertElements(
      List.of(FLEX_1x_8m),
      removeNoneOptimalPathsForStandardRaptor(List.of(FLEX_AND_WALK_1x_8m, FLEX_1x_8m))
    );
    // Flex+walk is faster, flex arrive on-board, both is optimal
    assertElements(
      List.of(FLEX_AND_WALK_1x_8m, FLEX_1x_10m),
      removeNoneOptimalPathsForStandardRaptor(List.of(FLEX_AND_WALK_1x_8m, FLEX_1x_10m))
    );

    // Walk has few rides, and Flex is faster - both is optimal
    assertElements(
      List.of(WALK_10m, FLEX_1x_8m),
      removeNoneOptimalPathsForStandardRaptor(List.of(WALK_10m, FLEX_1x_8m))
    );

    // Walk without opening hours is better than with, because it can be time-shifted without
    // any constraints
    assertElements(
      List.of(WALK_8m),
      removeNoneOptimalPathsForStandardRaptor(List.of(WALK_8m, WALK_W_OPENING_HOURS_8m))
    );

    // Walk with opening hours can NOT dominate another access/egress without - even if it is
    // faster. The reason is that it may not be allowed to time-shift it to the desired time.
    assertElements(
      List.of(WALK_10m, WALK_W_OPENING_HOURS_8m),
      removeNoneOptimalPathsForStandardRaptor(List.of(WALK_10m, WALK_W_OPENING_HOURS_8m))
    );
  }

  @Test
  void groupByRoundTest() {
    // Map one element
    var res = groupByRound(List.of(WALK_8m), e -> true);
    assertArrayEquals(new int[] { 0 }, res.keys());
    assertElements(List.of(WALK_8m), res.get(0));

    // Map 4 elements into 3 groups
    res = groupByRound(List.of(WALK_8m, FLEX_1x_8m, FLEX_2x_8m, FLEX_1x_10m), e -> true);
    int[] keys = res.keys();
    Arrays.sort(keys);

    assertArrayEquals(new int[] { 0, 1, 2 }, keys);
    assertElements(List.of(WALK_8m), res.get(0));
    assertElements(List.of(FLEX_1x_8m, FLEX_1x_10m), res.get(1));
    assertElements(List.of(FLEX_2x_8m), res.get(2));

    // Apply same test, but remove entries with number of rides == 1
    res = groupByRound(List.of(WALK_8m, FLEX_1x_8m), RaptorAccessEgress::hasRides);
    keys = res.keys();
    Arrays.sort(keys);

    assertArrayEquals(new int[] { 1 }, keys);
    assertElements(List.of(FLEX_1x_8m), res.get(1));
  }

  @Test
  void groupByStopTest() {
    // Map one element
    var res = groupByStop(List.of(WALK_8m));
    assertArrayEquals(new int[] { STOP }, res.keys());
    assertElements(List.of(WALK_8m), res.get(STOP));

    // Map 4 elements into 3 groups
    var walk_99 = TestAccessEgress.walk(99, D1s);
    res = groupByStop(List.of(WALK_8m, WALK_10m, walk_99));
    int[] keys = res.keys();
    Arrays.sort(keys);

    assertArrayEquals(new int[] { STOP, 99 }, keys);
    assertElements(List.of(WALK_8m, WALK_10m), res.get(STOP));
    assertElements(List.of(walk_99), res.get(99));
  }

  private static <T> void assertElements(Collection<T> expected, Collection<T> result) {
    assertEquals(
      expected.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")),
      result.stream().map(Object::toString).sorted().collect(Collectors.joining(", "))
    );
  }
}
