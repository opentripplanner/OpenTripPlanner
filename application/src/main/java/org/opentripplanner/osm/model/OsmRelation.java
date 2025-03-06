package org.opentripplanner.osm.model;

import java.util.ArrayList;
import java.util.List;

public class OsmRelation extends OsmEntity {

  private final List<OsmRelationMember> members = new ArrayList<>();

  public void addMember(OsmRelationMember member) {
    members.add(member);
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

  public boolean isLevelMap() {
    return isType("level_map");
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
