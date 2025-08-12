package org.opentripplanner.model;

import java.util.Arrays;

public enum FeedType {
  GTFS("GTFS"),
  NETEX("NeTEx");

  private final String value;

  FeedType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static FeedType of(String value) {
    return Arrays.stream(FeedType.values())
      .filter(ft -> ft.getValue().equals(value))
      .findFirst()
      .orElse(null);
  }
}
