package org.opentripplanner.openstreetmap.model;

public class OSMRelationMember {

  private OSMMemberType type;

  private long ref;

  private String role;

  public OSMMemberType getType() {
    return type;
  }

  public void setType(OSMMemberType type) {
    this.type = type;
  }

  public boolean hasType(OSMMemberType type) {
    return this.type == type;
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

  @Override
  public String toString() {
    return "osm rel " + type + ":" + role + ":" + ref;
  }
}
