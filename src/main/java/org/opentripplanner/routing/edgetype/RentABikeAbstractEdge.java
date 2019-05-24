package org.opentripplanner.routing.edgetype;

import java.util.Collections;
import java.util.Locale;
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

    protected State traverseRent(State state) {
        RoutingRequest options = state.getOptions();
        /*
         * If we already have a bike (rented or own) we won't go any faster by having a second one.
         */
        if (state.getNonTransitMode() != TraverseMode.WALK)
            return null;
        /*
         * To rent a bike, we need to have BICYCLE in allowed modes.
         */
        if (!options.modes.contains(TraverseMode.BICYCLE))
            return null;
        
        /*
         * To rent a bike, we need to have one of the networks in allowed networks
         */
        if (noBikeRentalNetworkAllowed(options.allowedBikeRentalNetworks))
            return null;
        
        BikeRentalStationVertex dropoff = (BikeRentalStationVertex) tov;
        if (options.useBikeRentalAvailabilityInformation && dropoff.getBikesAvailable() == 0)
            return null;
        
        StateEditor editor = state.edit(this);
        editor.incrementWeight(options.arriveBy ? options.bikeRentalDropoffCost : options.bikeRentalPickupCost);
        editor.incrementTimeInSeconds(options.arriveBy ? options.bikeRentalDropoffTime : options.bikeRentalPickupTime);
        editor.beginVehicleRenting(((BikeRentalStationVertex)fromv).getVehicleMode());
        editor.setBikeRentalNetworks(networks);
        editor.setBackMode(state.getNonTransitMode());
        return editor.makeState();
    }
    
    private boolean noBikeRentalNetworkAllowed(Set<String> allowedBikeRentalNetworks) {
        // allowedBikeRentalNetworks parameter is undefined -> allow all networks by default
        if (allowedBikeRentalNetworks == null)
            return false;
        
        // allowedBikeRentalNetworks parameter is defined but empty -> no networks are allowed
        if (allowedBikeRentalNetworks.isEmpty())
            return true;
        
        return Collections.disjoint(networks, allowedBikeRentalNetworks);
    }

    protected State traverseDropoff(State state) {
        RoutingRequest options = state.getOptions();
        
        /*
         * To drop off a bike, we need to have one of the networks in allowed networks
         */
        if (noBikeRentalNetworkAllowed(options.allowedBikeRentalNetworks))
            return null;
        
        /*
         * To dropoff a bike, we need to have rented one.
         */
        if (!state.isBikeRenting() || !hasCompatibleNetworks(networks, state.getBikeRentalNetworks()))
            return null;
        
        BikeRentalStationVertex pickup = (BikeRentalStationVertex) tov;
        if (options.useBikeRentalAvailabilityInformation && pickup.getSpacesAvailable() == 0)
            return null;
        
        StateEditor editor = state.edit(this);
        editor.incrementWeight(options.arriveBy ? options.bikeRentalPickupCost : options.bikeRentalDropoffCost);
        editor.incrementTimeInSeconds(options.arriveBy ? options.bikeRentalPickupTime : options.bikeRentalDropoffTime);
        editor.doneVehicleRenting();
        editor.setBackMode(TraverseMode.WALK);
        return editor.makeState();
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
