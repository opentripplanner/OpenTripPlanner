package org.opentripplanner.routing.algorithm.transferoptimization.model.costfilter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCostCalculator;

class MinCostPathTailFilterTest implements RaptorTestConstants {

  private static final TransferWaitTimeCostCalculator WAIT_TIME_CALC = new TransferWaitTimeCostCalculator(
    1.0,
    5.0
  );

  private final A v01 = new A("A", 0, 11);
  private final A v10 = new A("B", 1, 10);
  private final A v02 = new A("C", 0, 12);
  private final A w01 = new A("A'", 0, 11);

  @Test
  void filterEmptySet() {
    // filter empty set
    assertEquals(
      Set.of(),
      new MinCostPathTailFilter<TestTripSchedule>(List.of(OptimizedPathTail::generalizedCost))
        .filterIntermediateResult(Set.of(), 0)
    );
  }

  @Test
  void filterOneElement() {
    assertEquals(Set.of(v01), filter(v01));
  }

  @Test
  void filterTwoDistinctEntries() {
    assertEquals(Set.of(v01), filter(v01, v10));
    // swap order, should not matter
    assertEquals(Set.of(v01), filter(v10, v01));
  }

  @Test
  void filterTwoDistinctEntriesWithTheSameFirstValueX() {
    // Keep best y (x is same)
    assertEquals(Set.of(v01), filter(v01, v02));
    assertEquals(Set.of(v01), filter(v02, v01));
  }

  @Test
  void filterTwoEqualVectors() {
    assertEquals(Set.of(v01, w01), filter(v01, w01));
    assertEquals(Set.of(v01, w01), filter(w01, v01));
  }

  private Set<A> filter(A... as) {
    return new MinCostPathTailFilter<TestTripSchedule>(List.of(it -> ((A) it).x, it -> ((A) it).y))
      .filterIntermediateResult(Set.of(as), 0)
      .stream()
      .map(it -> (A) it)
      .collect(Collectors.toSet());
  }

  A toA(OptimizedPathTail<TestTripSchedule> e) {
    return (A) e;
  }

  static class A extends OptimizedPathTail<TestTripSchedule> {

    /** Name is included in eq/hc to be able to add the "same" [x,y] vector to a set. */
    public final String name;
    public final int x;
    public final int y;

    private A(String name, int x, int y) {
      super(SLACK_PROVIDER, COST_CALCULATOR, T00_00, WAIT_TIME_CALC, null, 0.0, null);
      this.name = name;
      this.x = x;
      this.y = y;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, y, name);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o instanceof A a) {
        return name.equals(a.name) && x == a.x && y == a.y;
      }
      return false;
    }

    @Override
    public String toString() {
      return name + "(" + x + ", " + y + ")";
    }
  }
}
