package org.opentripplanner.common.model;

public class NamedPlace {
    public String name;
    public String place;
    
    public NamedPlace(String name, String place) {
        this.name = name;
        this.place = place;
    }

    public NamedPlace(String place) {
        this.place = place;
    }
    
    public String toString() {
        return "NamedPlace(" + name + ", " + place + ")";
    }
}
