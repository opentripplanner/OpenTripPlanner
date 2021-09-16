package org.opentripplanner.routing.edgetype;

import java.util.Locale;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;

/**
 * Renting or dropping off a rented vehicle edge.
 * 
 * @author laurent
 * 
 */
public class VehicleRentalEdge extends Edge {

    private static final long serialVersionUID = 1L;

    public VehicleRentalEdge(VehicleRentalStationVertex vertex) {
        super(vertex, vertex);
    }

    public State traverse(State s0) {
        if (!s0.getOptions().bikeRental) { return null; }

        StateEditor s1 = s0.edit(this);
        RoutingRequest options = s0.getOptions();

        VehicleRentalStationVertex stationVertex = (VehicleRentalStationVertex) tov;
        String network = stationVertex.getStation().getNetwork();

        boolean pickedUp;
        if (options.arriveBy) {
            switch (s0.getVehicleRentalState()) {
                case BEFORE_RENTING:
                    return null;
                case HAVE_RENTED:
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getSpacesAvailable() == 0) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(stationVertex.getVehicleMode(), network, true);
                    pickedUp = false;
                    break;
                case RENTING_FLOATING:
                    if (stationVertex.getStation().isFloatingBike) {
                        s1.beginFloatingVehicleRenting(stationVertex.getVehicleMode(), network, true);
                        pickedUp = true;
                    } else {
                        return null;
                    }
                    break;
                case RENTING_FROM_STATION:
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getVehiclesAvailable() == 0) {
                        return null;
                    }
                    // For arriveBy searches mayKeepRentedVehicleAtDestination is only set in State#getInitialStates(),
                    // and so here it is checked if this bicycle could have been kept at the destination
                    if (s0.mayKeepRentedVehicleAtDestination() && !stationVertex.getStation().isKeepingVehicleRentalAtDestinationAllowed) {
                        return null;
                    }
                    if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) {  return null; }
                    s1.beginVehicleRentingAtStation(stationVertex.getVehicleMode(), network, false, true);
                    pickedUp = true;
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else {
            switch (s0.getVehicleRentalState()) {
                case BEFORE_RENTING:
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getVehiclesAvailable() == 0) {
                        return null;
                    }
                    if (stationVertex.getStation().isFloatingBike) {
                        s1.beginFloatingVehicleRenting(stationVertex.getVehicleMode(), network,
                                false);
                    } else {
                        var mayKeep =
                                stationVertex.getStation().isKeepingVehicleRentalAtDestinationAllowed
                                        && options.allowKeepingRentedVehicleAtDestination;
                        s1.beginVehicleRentingAtStation(stationVertex.getVehicleMode(), network,
                                mayKeep, false);
                    }
                    pickedUp = true;
                    break;
                case HAVE_RENTED:
                    return null;
                case RENTING_FLOATING:
                case RENTING_FROM_STATION:
                    if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) { return null; }
                    if (options.useVehicleRentalAvailabilityInformation && stationVertex.getSpacesAvailable() == 0) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(stationVertex.getVehicleMode(), network, false);
                    pickedUp = false;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        s1.incrementWeight(pickedUp ? options.bikeRentalPickupCost : options.bikeRentalDropoffCost);
        s1.incrementTimeInSeconds(pickedUp ? options.bikeRentalPickupTime : options.bikeRentalDropoffTime);
        s1.setBackMode(null);
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
     * @param stationNetwork The station network where we want to drop the bike off.
     * @param rentedNetwork The networks of the station we rented the bike from.
     * @return true if the bike can be dropped off here, false if not.
     */
    private boolean hasCompatibleNetworks(String stationNetwork, String rentedNetwork) {
        /*
         * Special case for "null" networks ("catch-all" network defined).
         */
        if (rentedNetwork == null) {
            return true;
        }

        return rentedNetwork.equals(stationNetwork);
    }
}
