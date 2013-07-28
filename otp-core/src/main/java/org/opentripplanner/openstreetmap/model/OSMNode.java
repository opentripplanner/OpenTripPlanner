/*
 Copyright 2008 Brian Ferris

 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.openstreetmap.model;

import lombok.Setter;
import lombok.Getter;

public class OSMNode extends OSMWithTags {

    @Setter
    @Getter
    private double lat;

    @Setter
    @Getter
    private double lon;

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

}
