package org.opentripplanner.osm.model;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.osm.OsmProvider;

public class OsmNodeBuilder {

  private static final Map<String, String> EMPTY_TAGS = null;
  private long id;
  private double lat;
  private double lon;
  // the vast majority of nodes don't have any tags, so we start with null, because that has no
  // allocations at all
  private Map<String, String> tags = EMPTY_TAGS;
  private OsmProvider osmProvider;

  public OsmNodeBuilder(OsmNode osmNode) {
    this.id = osmNode.id;
    this.lat = osmNode.lat;
    this.lon = osmNode.lon;
    this.tags = new HashMap<>();
    tags.putAll(osmNode.getTags());
    this.osmProvider = osmNode.getOsmProvider();
  }

  public OsmNodeBuilder() {}

  public OsmNodeBuilder withId(long id) {
    this.id = id;
    return this;
  }

  public OsmNodeBuilder withLatLon(double lat, double lon) {
    this.lat = lat;
    this.lon = lon;
    return this;
  }

  public OsmNodeBuilder withTag(String key, String value) {
    if (key != null && value != null) {
      if (this.tags == EMPTY_TAGS) {
        this.tags = new HashMap<>();
      }
      this.tags.put(key.toLowerCase(), value);
    }
    return this;
  }

  public OsmNodeBuilder withOsmProvider(OsmProvider osmProvider) {
    this.osmProvider = osmProvider;
    return this;
  }

  public OsmNode build() {
    var ret = new OsmNode(id, lat, lon, tags, osmProvider);
    this.tags = null;
    return ret;
  }
}
