package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.util.I18NString;

/**
 * Renting or dropping off a rented vehicle edge.
 * 
 * @author laurent
 * 
 */
public class VehicleRentalEdge extends Edge {

    private static final long serialVersionUID = 1L;
    public FormFactor formFactor;

    public VehicleRentalEdge(VehicleRentalStationVertex vertex, FormFactor formFactor) {
        super(vertex, vertex);
        this.formFactor = formFactor;
    }

    public State traverse(State s0) {
        if (!s0.getOptions().vehicleRental) { return null; }
        if (!s0.getOptions().allowedRentalFormFactors.isEmpty() &&
            !s0.getOptions().allowedRentalFormFactors.contains(formFactor)
        ) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        RoutingRequest options = s0.getOptions();

        VehicleRentalStationVertex stationVertex = (VehicleRentalStationVertex) tov;
        VehicleRentalPlace station = stationVertex.getStation();
        String network = station.getNetwork();
        boolean realtimeAvailability = options.useVehicleRentalAvailabilityInformation;

        if (station.networkIsNotAllowed(s0.getOptions())) {
            return null;
        }

        boolean pickedUp;
        if (options.arriveBy) {
            switch (s0.getVehicleRentalState()) {
                case BEFORE_RENTING:
                    return null;
                case HAVE_RENTED:
                    if (realtimeAvailability && (
                        !station.allowDropoffNow() ||
                        !station.getAvailableDropoffFormFactors(true).contains(formFactor)
                    )) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(formFactor, network, true);
                    pickedUp = false;
                    break;
                case RENTING_FLOATING:
                    if (realtimeAvailability &&
                        !station.getAvailablePickupFormFactors(true).contains(formFactor)
                    ) {
                        return null;
                    }
                    if (station.isFloatingVehicle()) {
                        s1.beginFloatingVehicleRenting(formFactor, network, true);
                        pickedUp = true;
                    } else {
                        return null;
                    }
                    break;
                case RENTING_FROM_STATION:
                    if (realtimeAvailability && (
                        !station.allowPickupNow() ||
                        !station.getAvailablePickupFormFactors(true).contains(formFactor)
                    )) {
                        return null;
                    }
                    // For arriveBy searches mayKeepRentedVehicleAtDestination is only set in State#getInitialStates(),
                    // and so here it is checked if this bicycle could have been kept at the destination
                    if (s0.mayKeepRentedVehicleAtDestination() && !station.isKeepingVehicleRentalAtDestinationAllowed()) {
                        return null;
                    }
                    if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) {  return null; }
                    s1.beginVehicleRentingAtStation(formFactor, network, false, true);
                    pickedUp = true;
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else {
            switch (s0.getVehicleRentalState()) {
                case BEFORE_RENTING:
                    if (realtimeAvailability && (
                        !station.allowPickupNow() ||
                        !station.getAvailablePickupFormFactors(true).contains(formFactor)
                    )) {
                        return null;
                    }
                    if (station.isFloatingVehicle()) {
                        s1.beginFloatingVehicleRenting(formFactor, network, false);
                    } else {
                        boolean mayKeep = options.allowKeepingRentedVehicleAtDestination && station.isKeepingVehicleRentalAtDestinationAllowed();
                        s1.beginVehicleRentingAtStation(formFactor, network, mayKeep, false);
                    }
                    pickedUp = true;
                    break;
                case HAVE_RENTED:
                    return null;
                case RENTING_FLOATING:
                case RENTING_FROM_STATION:
                    if (!hasCompatibleNetworks(network, s0.getVehicleRentalNetwork())) { return null; }
                    if (realtimeAvailability && (
                        !station.allowDropoffNow() ||
                        !station.getAvailableDropoffFormFactors(true).contains(formFactor)
                    )) {
                        return null;
                    }
                    if (!options.allowedRentalFormFactors.isEmpty() &&
                        station.getAvailableDropoffFormFactors(realtimeAvailability)
                            .stream()
                            .noneMatch(formFactor -> options.allowedRentalFormFactors.contains(formFactor))
                    ) {
                        return null;
                    }
                    s1.dropOffRentedVehicleAtStation(formFactor, network, false);
                    pickedUp = false;
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        s1.incrementWeight(pickedUp ? options.vehicleRentalPickupCost : options.vehicleRentalDropoffCost);
        s1.incrementTimeInSeconds(pickedUp ? options.vehicleRentalPickupTime : options.vehicleRentalDropoffTime);
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
    public I18NString getName() {
        return getToVertex().getName();
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
