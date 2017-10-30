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

import org.opentripplanner.model.AgencyAndId;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 */
public class FareComponent {

    public AgencyAndId fareId;
    public Money price;
    public List<AgencyAndId> routes;

    public FareComponent(AgencyAndId fareId, Money amount) {
        this.fareId = fareId;
        price = amount;
        routes = new ArrayList<AgencyAndId>();
    }

    public void addRoute(AgencyAndId routeId) {
        routes.add(routeId);
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("FareComponent(");
        buffer.append(fareId.toString());
        buffer.append(", ");
        buffer.append(price.toString());
        buffer.append(", ");
        for (AgencyAndId routeId : routes) {
            buffer.append(routeId.toString());
            buffer.append(", ");
        }
        buffer.append(")");
        return buffer.toString();
    }
}

