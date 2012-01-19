package org.opentripplanner.common.model;

/**
 * A starting/ending location for a trip.
 * @author novalis
 *
 */
public class NamedPlace {
    /** 
     * some human-readable text string e.g. W 34th St 
     * */
    public String name;
    /**  
     * "latitude,longitude", or the name of a graph vertex
     */
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
