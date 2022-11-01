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
  public String getOpenStreetMapLink() {
    return String.format("http://www.openstreetmap.org/relation/%d", getId());
  }
}
