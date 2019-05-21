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

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;

/**
 * Dropping off a rented vehicle edge.
 * 
 * Cost is the time to dropoff a vehicle.
 * 
 * @author Evan Siroky
 * 
 */
public class RentAVehicleOffEdge extends RentAVehicleAbstractEdge {

    private static final long serialVersionUID = 1L;

    public RentAVehicleOffEdge(Vertex v, VehicleRentalStation station) {
        super(v, station);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();

        if (!s0.isVehicleRentalDropoffAllowed(!station.isBorderDropoff))
            return null;

        // make sure there is at least one spot to park at the station
        if (options.useVehicleRentalAvailabilityInformation && station.spacesAvailable == 0)
            return null;

        StateEditor s1e = s0.edit(this);
        if (options.arriveBy) {
            s1e.beginVehicleRenting(0, station.networks, !station.isBorderDropoff);
        } else {
            s1e.endVehicleRenting();
        }
        s1e.incrementWeight(options.vehicleRentalDropoffCost);
        s1e.incrementTimeInSeconds(options.vehicleRentalDropoffTime);
        State s1 = s1e.makeState();
        return s1;
    }

    public boolean equals(Object o) {
        if (o instanceof RentAVehicleOffEdge) {
            RentAVehicleOffEdge other = (RentAVehicleOffEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "RentAVehicleOffEdge(" + fromv + " -> " + tov + ")";
    }
}
