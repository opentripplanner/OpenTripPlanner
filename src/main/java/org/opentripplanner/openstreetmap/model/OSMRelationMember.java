package org.opentripplanner.openstreetmap.model;

public class OSMRelationMember {

    private String type;

    private long ref;

    private String role;

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    @Override
    public String toString() {
        return "osm rel " + type + ":" + role + ":" + ref;
    }
}
