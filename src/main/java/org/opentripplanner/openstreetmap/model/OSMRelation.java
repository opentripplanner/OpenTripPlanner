package org.opentripplanner.openstreetmap.model;

import java.util.ArrayList;
import java.util.List;

public class OSMRelation extends OSMWithTags {

  private List<OSMRelationMember> _members = new ArrayList<OSMRelationMember>();

  public void addMember(OSMRelationMember member) {
    _members.add(member);
  }

  public List<OSMRelationMember> getMembers() {
    return _members;
  }

  public String toString() {
    return "osm relation " + id;
  }
}
