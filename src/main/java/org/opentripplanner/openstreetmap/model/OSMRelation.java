package org.opentripplanner.openstreetmap.model;

import java.util.ArrayList;
import java.util.List;

public class OSMRelation extends OSMWithTags {

  private List<OSMRelationMember> members = new ArrayList<OSMRelationMember>();

  public void addMember(OSMRelationMember member) {
    members.add(member);
  }

  public List<OSMRelationMember> getMembers() {
    return members;
  }

  public String toString() {
    return "osm relation " + id;
  }
}
