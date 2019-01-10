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

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Dropping off a rented car edge.
 * 
 * Cost is the time to dropoff a car.
 * 
 * @author Evan Siroky
 * 
 */
public class RentACarOffEdge extends RentACarAbstractEdge {

    private static final long serialVersionUID = 1L;

    public RentACarOffEdge(Vertex v, CarRentalStation station) {
        super(v, station);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();

        if (!s0.isCarRentalDropoffAllowed(!station.isBorderDropoff))
            return null;

        // make sure there is at least one spot to park at the station
        if (options.useCarRentalAvailabilityInformation && station.spacesAvailable == 0)
            return null;

        StateEditor s1e = s0.edit(this);
        if (options.arriveBy) {
            s1e.beginCarRenting(0, station.networks, !station.isBorderDropoff);
        } else {
            s1e.endCarRenting();
        }
        s1e.incrementWeight(options.carRentalDropoffCost);
        s1e.incrementTimeInSeconds(options.carRentalDropoffTime);
        State s1 = s1e.makeState();
        return s1;
    }

    public boolean equals(Object o) {
        if (o instanceof RentACarOffEdge) {
            RentACarOffEdge other = (RentACarOffEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "RentACarOffEdge(" + fromv + " -> " + tov + ")";
    }
}
