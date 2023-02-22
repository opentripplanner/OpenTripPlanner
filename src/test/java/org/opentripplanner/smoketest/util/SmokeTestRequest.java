package org.opentripplanner.smoketest.util;

import static java.util.Map.entry;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.smoketest.SmokeTest;

public record SmokeTestRequest(
  WgsCoordinate from,
  WgsCoordinate to,
  Collection<String> modes,
  boolean arriveBy
) {
  public SmokeTestRequest(WgsCoordinate from, WgsCoordinate to, Set<String> modes) {
    this(from, to, modes, false);
  }

  public Map<String, String> toMap() {
    return Map.ofEntries(
      entry("fromPlace", toString(from)),
      entry("toPlace", toString(to)),
      entry("time", "1:00pm"),
      entry("date", SmokeTest.nextMonday().toString()),
      entry("mode", String.join(",", modes)),
      entry("showIntermediateStops", "true"),
      entry("locale", "en"),
      entry("searchWindow", Long.toString(Duration.ofHours(2).toSeconds())),
      entry("arriveBy", Boolean.toString(arriveBy))
    );
  }

  private static String toString(WgsCoordinate c) {
    return "%s,%s".formatted(c.latitude(), c.longitude());
  }
}
