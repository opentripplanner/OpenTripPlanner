package org.opentripplanner.osm.model;

import java.util.Map;

/**
 * Test version of OsmEntity for use in tests.
 * All tags must be passed into the constructor.
 */
public class OsmTestEntity extends OsmEntity {

  public OsmTestEntity() {
    this(Map.of());
  }

  public OsmTestEntity(Map<String, String> tags) {
    super(0, tags, null);
  }

  public OsmTestEntity(String key, String value) {
    this(Map.of(key, value));
  }

  @SafeVarargs
  public OsmTestEntity(Map.Entry<String, String>... entries) {
    this(Map.ofEntries(entries));
  }
}
