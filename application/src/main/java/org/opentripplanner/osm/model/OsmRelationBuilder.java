package org.opentripplanner.osm.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.osm.OsmProvider;

public class OsmRelationBuilder {

  private long id;
  private Map<String, String> tags = new HashMap<>();
  private OsmProvider osmProvider;
  private List<OsmRelationMember> members = new ArrayList<>();

  public OsmRelationBuilder(OsmRelation osmRelation) {
    this.id = osmRelation.getId();
    this.tags = new HashMap<>(osmRelation.getTags());
    this.osmProvider = osmRelation.getOsmProvider();
    this.members = new ArrayList<>(osmRelation.getMembers());
  }

  public OsmRelationBuilder() {}

  public OsmRelationBuilder withId(long id) {
    this.id = id;
    return this;
  }

  public OsmRelationBuilder addTag(String key, String value) {
    if (key != null && value != null) {
      this.tags.put(key.toLowerCase(), value);
    }
    return this;
  }

  public OsmRelationBuilder withOsmProvider(OsmProvider osmProvider) {
    this.osmProvider = osmProvider;
    return this;
  }

  public OsmRelationBuilder addMember(OsmRelationMember member) {
    members.add(member);
    return this;
  }

  public OsmRelation build() {
    var ret = new OsmRelation(id, tags, osmProvider, members);
    this.tags = null;
    this.members = null;
    return ret;
  }
}
