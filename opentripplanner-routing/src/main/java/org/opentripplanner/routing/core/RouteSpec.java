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

/** 
 * A particular route as a user would see it for the purposes of multiple itineraries.
 */
public class RouteSpec implements Cloneable {
    public String agency;
    public String routeName;
    
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

    @Override
    public boolean equals(Object other) {
        if (other instanceof RouteSpec) {
            RouteSpec otherRs = (RouteSpec) other;
            return otherRs.agency.equals(agency) && otherRs.routeName.equals(routeName);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return agency.hashCode() ^ routeName.hashCode();
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
