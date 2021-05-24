package org.opentripplanner.routing.edgetype;

import com.google.common.collect.Sets;
import java.util.Locale;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;

/**
 * Renting or dropping off a rented bike edge.
 * 
 * @author laurent
 * 
 */
public class BikeRentalEdge extends Edge {

    private static final long serialVersionUID = 1L;

    private final Set<String> networks;

    public BikeRentalEdge(BikeRentalStationVertex vertex, Set<String> networks) {
        super(vertex, vertex);
        this.networks = networks;
    }

    public State traverse(State s0) {
        if (!s0.getOptions().bikeRental) { return null; }

        StateEditor s1 = s0.edit(this);
        RoutingRequest options = s0.getOptions();

        BikeRentalStationVertex stationVertex = (BikeRentalStationVertex) tov;

        boolean pickedUp;
        if (options.arriveBy) {
            switch (s0.getBikeRentalState()) {
                case BEFORE_RENTING:
                    return null;
                case HAVE_RENTED:
                    if (options.useBikeRentalAvailabilityInformation && stationVertex.getSpacesAvailable() == 0) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(stationVertex.getVehicleMode(), true);
                    pickedUp = false;
                    break;
                case RENTING_FLOATING:
                    if (stationVertex.getStation().isFloatingBike) {
                        s1.beginFloatingVehicleRenting(stationVertex.getVehicleMode(), true);
                        pickedUp = true;
                    } else {
                        return null;
                    }
                    break;
                case RENTING_FROM_STATION:
                    if (options.useBikeRentalAvailabilityInformation && stationVertex.getBikesAvailable() == 0) {
                        return null;
                    }
                    // For arriveBy searches mayKeepRentedBicycleAtDestination is only set in State#getInitialStates(),
                    // and so here it is checked if this bicycle could have been kept at the destination
                    if (s0.mayKeepRentedBicycleAtDestination() && !stationVertex.getStation().isKeepingBicycleRentalAtDestinationAllowed) {
                        return null;
                    }
                    if (!hasCompatibleNetworks(networks, s0.getBikeRentalNetworks())) { return null; }
                    s1.beginVehicleRentingAtStation(stationVertex.getVehicleMode(), false, true);
                    pickedUp = true;
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else {
            switch (s0.getBikeRentalState()) {
                case BEFORE_RENTING:
                    if (options.useBikeRentalAvailabilityInformation && stationVertex.getBikesAvailable() == 0) {
                        return null;
                    }
                    if (stationVertex.getStation().isFloatingBike) {
                        s1.beginFloatingVehicleRenting(stationVertex.getVehicleMode(), false);
                    } else {
                        var mayKeep =
                                stationVertex.getStation().isKeepingBicycleRentalAtDestinationAllowed
                                        && options.allowKeepingRentedBicycleAtDestination;
                        s1.beginVehicleRentingAtStation(stationVertex.getVehicleMode(), mayKeep, false);
                    }
                    pickedUp = true;
                    break;
                case HAVE_RENTED:
                    return null;
                case RENTING_FLOATING:
                case RENTING_FROM_STATION:
                    if (!hasCompatibleNetworks(networks, s0.getBikeRentalNetworks())) { return null; }
                    if (options.useBikeRentalAvailabilityInformation && stationVertex.getSpacesAvailable() == 0) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(stationVertex.getVehicleMode(), false);
                    pickedUp = false;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        s1.incrementWeight(pickedUp ? options.bikeRentalPickupCost : options.bikeRentalDropoffCost);
        s1.incrementTimeInSeconds(pickedUp ? options.bikeRentalPickupTime : options.bikeRentalDropoffTime);
        s1.setBikeRentalNetwork(networks);
        s1.setBackMode(s0.getNonTransitMode());
        return s1.makeState();
    }

    @Override
    public double getDistanceMeters() {
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
