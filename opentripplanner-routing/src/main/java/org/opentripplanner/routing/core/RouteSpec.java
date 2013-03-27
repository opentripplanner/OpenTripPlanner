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
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.ObjectUtils;

/** 
 * A particular route as a user would see it for the purposes of multiple itineraries.
 */
public class RouteSpec implements Cloneable, Serializable {
    private static final long serialVersionUID = 7053858697234679920L;

    public String agency;
    public String routeName;
    public String routeId;
    
    public RouteSpec(String agency, String routeName) {
        if (agency == null) {
            throw new IllegalArgumentException("Agency must not be null");
        }
        if (routeName == null) {
            throw new IllegalArgumentException("Route name must not be null");
        }
        this.agency = agency;
        this.routeName = routeName;
    }

    public RouteSpec(String agency, String routeName, String routeId) {
        if (agency == null) {
            throw new IllegalArgumentException("Agency must not be null");
        }
        if (routeName == null && routeId == null) {
            throw new IllegalArgumentException("Route name and Id must not be both null");
        }
        this.agency = agency;
        this.routeName = routeName;
        this.routeId = routeId;
    }

    public RouteSpec(String agencyAndRouteNameId) {
        String[] routeSpec = agencyAndRouteNameId.split("_", 3);
        if (routeSpec.length != 2 && routeSpec.length != 3) {
            throw new IllegalArgumentException("AgencyId or routeName/Id not set");
        }
        agency = routeSpec[0];
        routeName = routeSpec[1];
        if (routeName.length() == 0)
            routeName = null;
        routeId = routeSpec.length > 2 ? routeSpec[2] : null;
        if (routeName != null && routeName == null) {
            throw new IllegalArgumentException("Can't set both route name and ID");
        }
    }

    public static List<RouteSpec> listFromString(String list) {
        List<RouteSpec> ret = new LinkedList<RouteSpec>();
        for (String element : list.split(","))
            ret.add(new RouteSpec(element));
        return ret;
    }

    /**
     * Please note the specific contract for equals. Since we want to compare
     * routes based on name OR ids, but we sometime have a mix of one or both, we consider two routes
     * to be equals iff: agency ID is equals and route name and route id do not differs when defined.
     * 
     * This function should really be called something like "matches", but that would prevent us
     * from using the standard Java collection framework (map.contains(), ...)
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof RouteSpec) {
            RouteSpec otherRs = (RouteSpec) other;
            boolean routeNameDiffers = routeName != null && otherRs.routeName != null
                    && !routeName.equals(otherRs.routeName);
            boolean routeIdDiffers = routeId != null && otherRs.routeId != null
                    && !routeId.equals(otherRs.routeId);
            return otherRs.agency.equals(agency)
                    && !routeNameDiffers && !routeIdDiffers
                    && ObjectUtils.nullSafeEquals(routeName, otherRs.routeName)
                    || ObjectUtils.nullSafeEquals(routeId, otherRs.routeId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int retval = agency.hashCode();
        /*
         * Since different routeName+routeId can be equals, we can't include them in the hashCode() function to respect hashCode/equals contract (see
         * equals).
         */
        return retval;
    }

    public String getRepresentation() {
        return agency + "_" + (routeName != null ? routeName : "")
                + (routeId != null ? "_" + routeId : "");
    }

    @Override
    public String toString() {
    	return String.format("RouteSpec<agency=%s name=%s id=%s>", agency, routeName, routeId);
    }

    public RouteSpec clone() {
        try {
            return (RouteSpec) super.clone();
        } catch (CloneNotSupportedException e) {
            /* this will never happen since our super is the cloneable object */
            throw new RuntimeException(e); 
        }
    }
}
