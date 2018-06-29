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
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.CarRentalStationVertex;

import java.util.Locale;
import java.util.Set;

/**
 * Renting or dropping off a rented car edge.
 * 
 * @author Evan Siroky
 * 
 */
public abstract class RentACarAbstractEdge extends Edge {

    private static final long serialVersionUID = 1L;

    private Set<String> networks;

    public RentACarAbstractEdge(Vertex from, Vertex to, Set<String> networks) {
        super(from, to);
        this.networks = networks;
    }

    protected State traverseRent(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * If we already have a car (rented or own) we won't go any faster by having a second one.
         */
        if (!s0.getNonTransitMode().equals(TraverseMode.WALK))
            return null;
        /*
         * To rent a car, we need to have BICYCLE in allowed modes.
         */
        if (!options.modes.contains(TraverseMode.BICYCLE))
            return null;

        CarRentalStationVertex dropoff = (CarRentalStationVertex) tov;
        if (options.useCarRentalAvailabilityInformation && dropoff.getCarsAvailable() == 0) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(options.arriveBy ? options.carRentalDropoffCost
                : options.carRentalPickupCost);
        s1.incrementTimeInSeconds(options.arriveBy ? options.carRentalDropoffTime
                : options.carRentalPickupTime);
        s1.setCarRenting(true);
        s1.setCarRentalNetwork(networks);
        s1.setBackMode(s0.getNonTransitMode());
        State s1b = s1.makeState();
        return s1b;
    }

    protected State traverseDropoff(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * To dropoff a car, we need to have rented one.
         */
        if (!s0.isCarRenting() || !hasCompatibleNetworks(networks, s0.getCarRentalNetworks()))
            return null;
        CarRentalStationVertex pickup = (CarRentalStationVertex) tov;
        if (options.useCarRentalAvailabilityInformation && pickup.getSpacesAvailable() == 0) {
            return null;
        }

        StateEditor s1e = s0.edit(this);
        s1e.incrementWeight(options.arriveBy ? options.carRentalPickupCost
                : options.carRentalDropoffCost);
        s1e.incrementTimeInSeconds(options.arriveBy ? options.carRentalPickupTime
                : options.carRentalDropoffTime);
        s1e.setCarRenting(false);
        s1e.setBackMode(TraverseMode.WALK);
        State s1 = s1e.makeState();
        return s1;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public String getName() {
        return getToVertex().getName();
    }

    @Override
    public String getName(Locale locale) {
        return getToVertex().getName(locale);
    }

    @Override
    public boolean hasBogusName() {
        return false;
    }

    /**
     * @param stationNetworks The station where we want to drop the car off.
     * @param rentedNetworks The set of networks of the station we rented the car from.
     * @return true if the car can be dropped off here, false if not.
     */
    private boolean hasCompatibleNetworks(Set<String> stationNetworks, Set<String> rentedNetworks) {
        /*
         * Two stations are compatible if they share at least one network. Special case for "null"
         * networks ("catch-all" network defined).
         */
        if (stationNetworks == null || rentedNetworks == null)
            return true; // Always a match
        return !Sets.intersection(stationNetworks, rentedNetworks).isEmpty();
    }
}
