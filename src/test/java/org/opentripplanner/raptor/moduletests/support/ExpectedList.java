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
    return fromTo(0, n);
  }

  public String get(int index) {
    return items[index];
  }

  public String[] get(int... indexes) {
    return Arrays.stream(indexes).mapToObj(i -> items[i]).toList().toArray(new String[0]);
  }

  public String last() {
    return items[items.length - 1];
  }

  public String[] last(int n) {
    return fromTo(items.length - n, items.length);
  }

  public String[] fromTo(int startInclusive, int endExclusive) {
    return Arrays.stream(items, startInclusive, endExclusive).toList().toArray(new String[0]);
  }
}
