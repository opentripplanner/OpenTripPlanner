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

import com.google.common.collect.Sets;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;

/**
 * Renting a vehicle edge.
 * 
 * Cost is the time to pickup a vehicle plus "inconvenience of renting".
 * 
 * @author Evan Siroky
 * 
 */
public class RentAVehicleOnEdge extends RentAVehicleAbstractEdge {

    private static final long serialVersionUID = 1L;

    public RentAVehicleOnEdge(VehicleRentalStationVertex v, VehicleRentalStation station) {
        super(v, station);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();

        // To rent a vehicle, we need to have vehicle rental allowed in request.
        if (!options.allowVehicleRental)
            return null;

        // make sure there is at least one vehicle available to rent at this station
        if (options.useVehicleRentalAvailabilityInformation && station.vehiclesAvailable == 0)
            return null;

        // don't use the same pickup station twice
        if (s0.stateData.getRentedVehicles().contains(station.id))
            return null;

        // make sure the vehicle being rented is within a network compatible with the request
        if (
            !hasCompatibleNetworks(
                options.companies != null
                    ? Sets.newHashSet(options.companies.split(","))
                    : null,
                station.networks
            )
        ) {
            return null;
        }

        // make sure only one vehicle is rented pre/post transit
        if (s0.isEverBoarded()) {
            if (s0.stateData.hasRentedVehiclePostTransit()) {
                return null;
            }
        } else {
            if (s0.stateData.hasRentedVehiclePreTransit()) {
                return null;
            }
        }

        StateEditor s1e = s0.edit(this);
        if (options.arriveBy) {
            // In an "arrive by" search, a vehicle may have already been dropped off and this potential
            // pickup location has been encountered.

            // First make sure the current state has a vehicle rented.
            if (!s0.isVehicleRenting())
                return null;

            // Check if the vehicle network at this edge is compatible with the allowable vehicle networks
            // where the vehicle was dropped off.  Dropoff points could be either a dropoff station or a
            // StreetEdge for floating vehicle rentals.
            if (!hasCompatibleNetworks(s0.getVehicleRentalNetworks(), station.networks)) {
                // The networks found at the dropoff point are incompatible with those found at
                // this pickup station, so return null.
                return null;
            }

            // make sure the minimum vehicle rental distance has been traveled
            if (s0.vehicleRentalDistance < options.minimumVehicleRentalDistance)
                return null;

            // make sure the vehicle that is about to be rented has floating dropoff capabilities if it
            // was dropped off in a floating state
            if (s0.stateData.rentedVehicleAllowsFloatingDropoffs() && !station.isFloatingVehicle)
                return null;

            // make sure only one vehicle is rented pre/post transit
            if (s0.isEverBoarded()) {
                if (s0.stateData.hasRentedVehiclePreTransit()) {
                    return null;
                }
            } else {
                if (s0.stateData.hasRentedVehiclePostTransit()) {
                    return null;
                }
            }

            // looks like it's ok to have rented a vehicle from this station
            s1e.endVehicleRenting();
        } else {
            // make sure more than 1 vehicle isn't rented at once
            if (s0.isVehicleRenting())
                return null;

            s1e.beginVehicleRenting(0, station.networks, station.isFloatingVehicle);
        }
        s1e.incrementWeight(options.vehicleRentalPickupCost);
        s1e.incrementTimeInSeconds(options.vehicleRentalPickupTime);
        s1e.addRentedVehicle(station.id);
        State s1 = s1e.makeState();
        return s1;
    }

    public boolean equals(Object o) {
        if (o instanceof RentAVehicleOnEdge) {
            RentAVehicleOnEdge other = (RentAVehicleOnEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "RentAVehicleOnEdge(" + fromv + " -> " + tov + ")";
    }
}
