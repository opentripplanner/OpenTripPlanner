package org.opentripplanner.osm.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opentripplanner.osm.OsmProvider;

public class OsmRelation extends OsmEntity {

  private final List<OsmRelationMember> members;

  OsmRelation(
    long id,
    Map<String, String> tags,
    OsmProvider osmProvider,
    List<OsmRelationMember> members
  ) {
    super(id, tags, osmProvider);
    this.members = Collections.unmodifiableList(members);
  }

  public static OsmRelationBuilder of() {
    return new OsmRelationBuilder();
  }

  public OsmRelationBuilder copy() {
    return new OsmRelationBuilder(this);
  }

  public List<OsmRelationMember> getMembers() {
    return members;
  }

  public String toString() {
    return "osm relation " + id;
  }

  @Override
  public String url() {
    return String.format("https://www.openstreetmap.org/relation/%d", getId());
  }

  public boolean isBicycleRoute() {
    return isRoute() && isTag("route", "bicycle");
  }

  public boolean isRoute() {
    return isType("route");
  }

  public boolean isRoadRoute() {
    return isRoute() && isTag("route", "road");
  }

  public boolean isRestriction() {
    return isType("restriction");
  }

  public boolean isPublicTransport() {
    return isType("public_transport");
  }

  public boolean isMultiPolygon() {
    return isType("multipolygon");
  }

  public boolean isStopArea() {
    return isPublicTransport() && isTag("public_transport", "stop_area");
  }

  private boolean isType(String type) {
    return isTag("type", type);
  }
}
