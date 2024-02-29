package org.opentripplanner.raptor.moduletests.support;

import java.util.Arrays;

public class ExpectedList {

  private final String[] items;

  public ExpectedList(String... items) {
    this.items = items;
  }

  public String[] all() {
    return items;
  }

  public String first() {
    return items[0];
  }

  public String[] first(int n) {
    return range(0, n);
  }

  public String last() {
    return items[items.length - 1];
  }

  public String[] last(int n) {
    return range(items.length - n, items.length);
  }

  public String get(int index) {
    return items[index];
  }

  public String[] get(int... indexes) {
    return Arrays.stream(indexes).mapToObj(i -> items[i]).toList().toArray(new String[0]);
  }

  public String[] range(int startInclusive, int endExclusive) {
    return Arrays.stream(items, startInclusive, endExclusive).toList().toArray(new String[0]);
  }
}
