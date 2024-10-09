package org.opentripplanner.openstreetmap.model;

import java.util.ArrayList;
import java.util.List;

public class OSMRelation extends OSMWithTags {

  private final List<OSMRelationMember> members = new ArrayList<>();

  public void addMember(OSMRelationMember member) {
    members.add(member);
  }

  public List<OSMRelationMember> getMembers() {
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
