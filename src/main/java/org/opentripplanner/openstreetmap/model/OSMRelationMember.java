package org.opentripplanner.openstreetmap.model;

import static org.opentripplanner.openstreetmap.model.OsmMemberType.WAY;

public class OSMRelationMember {

  private OsmMemberType type;

  private long ref;

  private String role;

  public OsmMemberType getType() {
    return type;
  }

  public void setType(OsmMemberType type) {
    this.type = type;
  }

  public long getRef() {
    return ref;
  }

  public void setRef(long ref) {
    this.ref = ref;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public boolean hasRoleOuter() {
    return "outer".equals(role);
  }

  public boolean hasRoleInner() {
    return "inner".equals(role);
  }

  public boolean hasRolePlatform() {
    return "platform".equals(role);
  }

  public boolean hasTypeWay() {
    return type == WAY;
  }

  @Override
  public String toString() {
    return "osm rel " + type + ":" + role + ":" + ref;
  }
}
