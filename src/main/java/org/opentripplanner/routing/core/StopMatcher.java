/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashSet;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;

/**
 * A StopMatcher is a collection of stops based on IDs and agency IDs.
 * 
 * We currently only support full stop IDs (agencyId:stopId).
 * Support for other matching expression (or other types of stop banning) can be easily added later on.
 */
public class StopMatcher implements Cloneable, Serializable {
    private static final long serialVersionUID = 1274704742132971135L;

    /**
     * Set of full matching stop ids (agency ID: + stop ID)
     */
    private HashSet<AgencyAndId> agencyAndStopIds = new HashSet<AgencyAndId>();

    private StopMatcher() {
    }

    /**
     * Return an empty matcher (which matches no stops).
     */
    public static StopMatcher emptyMatcher() {
        return new StopMatcher();
    }
    
    /**
     * Returns whether this matcher is empty 
     * @return true when this matcher is empty, false otherwise
     */
    public boolean isEmpty() {
        return agencyAndStopIds.isEmpty();
    }

    /**
     * Build a new StopMatcher from a string representation.
     * 
     * @param stopList is a comma-separated list of stops, each of the format [agencyId]:[stopId]
     * @return A StopMatcher
     * @throws IllegalArgumentException if the string representation is invalid.
     */
    public static StopMatcher parse(String stopList) {
        if (stopList == null)
            return emptyMatcher();
        StopMatcher retval = new StopMatcher();
        int n = 0;
        for (String stopString : stopList.split(",")) {
            if (stopString.isEmpty()) {
                continue;
            }
            n++;

            try {
                AgencyAndId stopId = GtfsLibrary.convertIdFromString(stopString);
                retval.agencyAndStopIds.add(stopId);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Wrong stop spec format: " + stopString);
            }
        }
        if (n == 0) {
            return emptyMatcher();
        }
        return retval;
    }

    /**
     * Function to determine whether this StopMatcher matches a particular stop.
     * When a stop has a parent stop, it is also matched when its parent stop is matched.
     * @param stop is the stop to match using its ID
     * @return true when the stop is matched
     */
    public boolean matches(Stop stop) {
        // Don't bother with an empty matcher 
        if (this.isEmpty()) {
            return false;
        }
        else if (stop != null) {
            // Check whether stop is matched
            if (matches(stop.getId())) {
                return true;    
            }
            // Check whether parent stop is matched
            else if (stop.getParentStation() != null 
                    && !stop.getParentStation().isEmpty()) {
                // This stop has a parent
                AgencyAndId parentId = new AgencyAndId(stop.getId().getAgencyId(), stop.getParentStation());
                if (matches(parentId)) {
                    return true;    
                }
            }
        }
        return false;
    }
    
    /**
     * Function to determine whether this StopMatcher matches a particular stop id.
     * Warning: this function does not check for parent stops.
     * @param stopId is the stop id
     * @return true when stop id is matched 
     */
    private boolean matches(AgencyAndId stopId) {
        if (agencyAndStopIds.contains(stopId)) {
            return true;    
        }
        return false;
    }
    
    /**
     * Returns string representation of this matcher
     * @return string representation of this matcher
     */
    public String asString() {
        StringBuilder builder = new StringBuilder();
        for (AgencyAndId agencyAndId : agencyAndStopIds) {
            builder.append(agencyAndId.toString());
            builder.append(",");
        }
        // Remove last comma
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return String.format(
                "StopMatcher<agencyAndStopIds=%s>",
                agencyAndStopIds);
    }

    @Override
    public boolean equals(Object another) {
        if (another == null || !(another instanceof StopMatcher)) {
            return false;
        }
        if (another == this) {
            return true;
        }
        StopMatcher anotherMatcher = (StopMatcher) another;
        return agencyAndStopIds.equals(anotherMatcher.agencyAndStopIds);
    }

    @Override
    public int hashCode() {
        return agencyAndStopIds.hashCode();
    }

    @Override
    public StopMatcher clone() {
        try {
            return (StopMatcher) super.clone();
        } catch (CloneNotSupportedException e) {
            /* this will never happen since our super is the cloneable object */
            throw new RuntimeException(e);
        }
    }
}
