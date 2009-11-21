package org.opentripplanner.graph_builder.model.osm;

public class OSMRelationMember {

  private String type;

  private int ref;

  private String role;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getRef() {
    return ref;
  }

  public void setRef(int ref) {
    this.ref = ref;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
