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

import org.springframework.util.ObjectUtils;

/**
 * A particular route as a user would see it for the purposes of multiple itineraries.
 * 
 * TODO This class seems to be used only in GraphPath.getRouteSpecs() which in turn is never called... To remove?
 */
public class RouteSpec implements Cloneable, Serializable {
    private static final long serialVersionUID = 7053858697234679920L;

    public String agency;

    public String routeName;

    public String routeId;

    public RouteSpec(String agency, String routeName, String routeId) {
        if (agency == null || routeName == null || routeId == null) {
            throw new IllegalArgumentException("Must specify all parameters");
        }
        this.agency = agency;
        this.routeName = routeName;
        this.routeId = routeId;
    }

    /**
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof RouteSpec) {
            RouteSpec otherRs = (RouteSpec) other;
            return ObjectUtils.nullSafeEquals(agency, otherRs.agency)
                    && ObjectUtils.nullSafeEquals(routeName, otherRs.routeName)
                    && ObjectUtils.nullSafeEquals(routeId, otherRs.routeId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int retval = agency.hashCode() + routeName.hashCode() + routeId.hashCode();
        return retval;
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
