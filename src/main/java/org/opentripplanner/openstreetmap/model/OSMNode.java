package org.opentripplanner.openstreetmap.model;

public class OSMNode extends OSMWithTags {

    public double lat;
    public double lon;

    public String toString() {
        return "osm node " + id;
    }

    /**
     * Returns the capacity of this node if defined, or 0.
     * 
     * @return
     */
    public int getCapacity() throws NumberFormatException {
        String capacity = getTag("capacity");
        if (capacity == null) {
            return 0;
        }
        
        return Integer.parseInt(getTag("capacity"));
    }

    /**
     * Is this a multi-level node that should be decomposed to multiple coincident nodes? Currently returns true only for elevators.
     * 
     * @return whether the node is multi-level
     * @author mattwigway
     */
    public boolean isMultiLevel() {
        return hasTag("highway") && "elevator".equals(getTag("highway"));
    }
    
    public boolean hasTrafficLight() {
        return hasTag("highway") && "traffic_signals".equals(getTag("highway"));
    }

    /**
     * Is this a public transport stop that can be linked to a transit stop vertex later on.
     *
     * @return whether the node is a transit stop
     * @author hannesj
     */
    public boolean isStop() {
        return "bus_stop".equals(getTag("highway"))
                || "tram_stop".equals(getTag("railway"))
                || "station".equals(getTag("railway"))
                || "halt".equals(getTag("railway"))
                || "bus_station".equals(getTag("amenity"));
    }

    /**
     * TODO Maybe all those methods (isSomething...) in OSMXxx should be moved to a dedicated OSM
     * filtering class.
     * 
     * @return True if this node is a bike rental station.
     */
    public boolean isBikeRental() {
        return isTag("amenity", "bicycle_rental");
    }

    /**
     * Checks if this node is bollard
     * @return true if it is
     */
    public boolean isBollard() {
        return isTag("barrier", "bollard");
    }
}
