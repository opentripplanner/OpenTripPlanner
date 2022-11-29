package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.tostring.ValueObjectToStringBuilder;

class MinCostFilterChainTest {

  private final A v01 = new A("A", 0, 1);
  private final A v10 = new A("B", 1, 0);
  private final A v02 = new A("C", 0, 2);
  private final A w01 = new A("A'", 0, 1);

  static Set<A> setOf(A... as) {
    return Set.of(as);
  }

  @Test
  void filterEmptySet() {
    // filter empty set
    assertEquals(Set.of(), new MinCostFilterChain<A>(List.of(it -> it.x)).filter(Set.of()));
  }

  @Test
  void filterOneElement() {
    assertEquals(setOf(v01), filter(v01));
  }

  @Test
  void filterTwoDistinctEntries() {
    assertEquals(setOf(v01), filter(v01, v10));
    // swap order, should not matter
    assertEquals(setOf(v01), filter(v10, v01));
  }

  @Test
  void filterTwoDistinctEntriesWithTheSameFirstValueX() {
    // Keep best y (x is same)
    assertEquals(setOf(v01), filter(v01, v02));
    assertEquals(setOf(v01), filter(v02, v01));
  }

  @Test
  void filterTwoEqualVectors() {
    assertEquals(setOf(v01, w01), filter(v01, w01));
    assertEquals(setOf(v01, w01), filter(w01, v01));
  }

  private Set<A> filter(A... as) {
    return new MinCostFilterChain<A>(List.of(it -> it.x, it -> it.y)).filter(setOf(as));
  }

  static class A {

    /** Name is included in eq/hc to be able to add the "same" [x,y] vector to a set. */
    public final String name;
    public final int x;
    public final int y;

    private A(String name, int x, int y) {
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
      if (!(o instanceof A)) {
        return false;
      }
      final A a = (A) o;
      return name.equals(a.name) && x == a.x && y == a.y;
    }

    @Override
    public String toString() {
      return ValueObjectToStringBuilder
        .of()
        .addText(name)
        .addText("(")
        .addNum(x)
        .addText(", ")
        .addNum(y)
        .addText(")")
        .toString();
    }
  }
}
