package org.opentripplanner.smoketest.util;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import org.opentripplanner.smoketest.SmokeTest;
import org.opentripplanner.transit.model.basic.WgsCoordinate;

public record SmokeTestRequest(WgsCoordinate from, WgsCoordinate to, Collection<String> modes) {
  public Map<String, String> toMap() {
    return Map.of(
      "fromPlace",
      toString(from),
      "toPlace",
      toString(to),
      "time",
      "1:00pm",
      "date",
      SmokeTest.nextMonday().toString(),
      "mode",
      String.join(",", modes),
      "showIntermediateStops",
      "true",
      "locale",
      "en",
      "searchWindow",
      Long.toString(Duration.ofHours(2).toSeconds())
    );
  }

  private static String toString(WgsCoordinate c) {
    return "%s,%s".formatted(c.latitude(), c.longitude());
  }
}
