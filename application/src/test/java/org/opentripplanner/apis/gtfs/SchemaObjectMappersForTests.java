package org.opentripplanner.apis.gtfs;

import static java.util.Map.entry;

import java.util.Map;

public class SchemaObjectMappersForTests {

  public static Map<String, Double> mapCoordinate(double latitude, double longitude) {
    return Map.ofEntries(entry("latitude", latitude), entry("longitude", longitude));
  }
}
