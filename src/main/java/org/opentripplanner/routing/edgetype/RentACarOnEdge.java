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
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.vertextype.CarRentalStationVertex;

/**
 * Renting a car edge.
 *
 * Cost is the time to pickup a car plus "inconvenience of renting".
 *
 * @author Evan Siroky
 *
 */
public class RentACarOnEdge extends RentACarAbstractEdge {

    private static final long serialVersionUID = 1L;

    public RentACarOnEdge(CarRentalStationVertex v, CarRentalStation station) {
        super(v, station);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();

        // To rent a car, we need to have car rental allowed in request.
        if (!options.allowCarRental)
            return null;

        // make sure there is at least one car available to rent at this station
        if (options.useCarRentalAvailabilityInformation && station.carsAvailable == 0)
            return null;

        // don't use the same pickup station twice
        if (s0.stateData.getRentedCars().contains(station.id))
            return null;

        // make sure the car being rented is within a network compatible with the request
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

        StateEditor s1e = s0.edit(this);
        if (options.arriveBy) {
            // In an "arrive by" search, a car may have already been dropped off and this potential
            // pickup location has been encountered.

            // First make sure the current state has a car rented.
            if (!s0.isCarRenting())
                return null;

            // Check if the car network at this edge is compatible with the allowable car networks
            // where the car was dropped off.  Dropoff points could be either a dropoff station or a
            // StreetEdge for floating car rentals.
            if (!hasCompatibleNetworks(s0.getCarRentalNetworks(), station.networks)) {
                // The networks found at the dropoff point are incompatible with those found at
                // this pickup station, so return null.
                return null;
            }

            // make sure the minimum car rental distance has been traveled
            if (s0.carRentalDriveDistance < options.minimumCarRentalDistance)
                return null;

            // make sure the car that is about to be rented has floating dropoff capabilities if it
            // was dropped off in a floating state
            if (s0.stateData.rentedCarAllowsFloatingDropoffs() && !station.isFloatingCar)
                return null;

            // looks like it's ok to have rented a car from this station
            s1e.endCarRenting();
        } else {
            // make sure more than 1 car isn't rented at once
            if (s0.isCarRenting())
                return null;

            s1e.beginCarRenting(0, station.networks, station.isFloatingCar);
        }
        s1e.incrementWeight(options.carRentalPickupCost);
        s1e.incrementTimeInSeconds(options.carRentalPickupTime);
        s1e.addRentedCar(station.id);
        State s1 = s1e.makeState();
        return s1;
    }

    public boolean equals(Object o) {
        if (o instanceof RentACarOnEdge) {
            RentACarOnEdge other = (RentACarOnEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "RentACarOnEdge(" + fromv + " -> " + tov + ")";
    }
}
