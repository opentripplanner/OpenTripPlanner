package org.opentripplanner.osm.model;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.osm.OsmProvider;

public class OsmWayBuilder {

  private long id;
  private Map<String, String> tags = new HashMap<>();
  private OsmProvider osmProvider;
  private TLongList nodes = new TLongArrayList();

  public OsmWayBuilder() {}

  public OsmWayBuilder(OsmWay way) {
    this.id = way.getId();
    this.tags.putAll(way.getTags());
    this.osmProvider = way.getOsmProvider();
    this.nodes.addAll(way.getNodeRefs());
  }

  public OsmWayBuilder withId(long id) {
    this.id = id;
    return this;
  }

  public OsmWayBuilder withTag(String key, String value) {
    if (key != null && value != null) {
      this.tags.put(key.toLowerCase(), value);
    }
    return this;
  }

  public OsmWayBuilder withOsmProvider(OsmProvider osmProvider) {
    this.osmProvider = osmProvider;
    return this;
  }

  public OsmWayBuilder addNodeRef(long... nodeRefs) {
    for (long nodeRef : nodeRefs) {
      nodes.add(nodeRef);
    }
    return this;
  }

  public OsmWayBuilder insertNodeRef(long nodeRef, int index) {
    nodes.insert(index, nodeRef);
    return this;
  }

  public OsmWay build() {
    var ret = new OsmWay(id, tags, osmProvider, nodes);
    this.tags = null;
    this.nodes = null;
    return ret;
  }
}
