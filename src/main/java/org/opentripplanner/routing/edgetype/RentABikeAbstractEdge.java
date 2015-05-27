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

import java.util.Set;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/**
 * Renting or dropping off a rented bike edge.
 * 
 * @author laurent
 * 
 */
public abstract class RentABikeAbstractEdge extends Edge {

    private static final long serialVersionUID = 1L;

    private Set<String> networks;

    public RentABikeAbstractEdge(Vertex from, Vertex to, Set<String> networks) {
        super(from, to);
        this.networks = networks;
    }

    protected State traverseRent(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * If we already have a bike (rented or own) we won't go any faster by having a second one.
         */
        if (!s0.getNonTransitMode().equals(TraverseMode.WALK))
            return null;
        /*
         * To rent a bike, we need to have BICYCLE in allowed modes.
         */
        if (!options.modes.contains(TraverseMode.BICYCLE))
            return null;

        BikeRentalStationVertex dropoff = (BikeRentalStationVertex) tov;
        if (options.useBikeRentalAvailabilityInformation && dropoff.getBikesAvailable() == 0) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(options.arriveBy ? options.bikeRentalDropoffCost
                : options.bikeRentalPickupCost);
        s1.incrementTimeInSeconds(options.arriveBy ? options.bikeRentalDropoffTime
                : options.bikeRentalPickupTime);
        s1.setBikeRenting(true);
        s1.setBikeRentalNetwork(networks);
        s1.setBackMode(s0.getNonTransitMode());
        State s1b = s1.makeState();
        return s1b;
    }

    protected State traverseDropoff(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * To dropoff a bike, we need to have rented one.
         */
        if (!s0.isBikeRenting() || !hasCompatibleNetworks(networks, s0.getBikeRentalNetworks()))
            return null;
        BikeRentalStationVertex pickup = (BikeRentalStationVertex) tov;
        if (options.useBikeRentalAvailabilityInformation && pickup.getSpacesAvailable() == 0) {
            return null;
        }

        StateEditor s1e = s0.edit(this);
        s1e.incrementWeight(options.arriveBy ? options.bikeRentalPickupCost
                : options.bikeRentalDropoffCost);
        s1e.incrementTimeInSeconds(options.arriveBy ? options.bikeRentalPickupTime
                : options.bikeRentalDropoffTime);
        s1e.setBikeRenting(false);
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
     * @param stationNetworks The station where we want to drop the bike off.
     * @param rentedNetworks The set of networks of the station we rented the bike from.
     * @return true if the bike can be dropped off here, false if not.
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
