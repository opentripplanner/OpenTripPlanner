package org.opentripplanner.routing.core;

import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;

/**
 * StateData contains the components of search state that are unlikely to be changed as often as
 * time or weight. This avoids frequent duplication, which should have a positive impact on both
 * time and space use during searches.
 */
public class StateData implements Cloneable {

    // the time at which the search started
    protected long startTime;

    // TODO OTP2 Many of these could be replaced by a more generic state machine implementation

    protected boolean vehicleParked;

    protected VehicleRentalState vehicleRentalState;

    protected boolean mayKeepRentedVehicleAtDestination;

    protected CarPickupState carPickupState;

    /**
     * Time restrictions encountered while traversing edges.
     */
    protected List<TimeRestrictionWithOffset> timeRestrictions;

    /**
     * The sources of the time restrictions. This is used for state domination, so that
     * <p>
     * 1) it is possible to have two {@link org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress}es
     * to the same stop, but with different {@link org.opentripplanner.routing.vehicle_parking.VehicleParking}
     * entities.
     *
     * 2) states with the same restriction-source don't dominate each other.
     */
    protected Set<Object> timeRestrictionSources;

    protected RoutingRequest opt;

    /**
     * The preferred mode, which may differ from backMode when for example walking with a bike.
     * It may also change during traversal when switching between modes as in the case of Park & Ride or Kiss & Ride.
     */
    protected TraverseMode currentMode;

    /**
     * The mode that was used to traverse the backEdge
     */
    protected TraverseMode backMode;

    protected boolean backWalkingBike;

    public String vehicleRentalNetwork;

    public FormFactor rentalVehicleFormFactor;

    /* This boolean is set to true upon transition from a normal street to a no-through-traffic street. */
    protected boolean enteredNoThroughTrafficArea;

    public StateData(RoutingRequest options) {
        TraverseModeSet modes = options.streetSubRequestModes;
        if (modes.getCar())
            currentMode = TraverseMode.CAR;
        else if (modes.getWalk())
            currentMode = TraverseMode.WALK;
        else if (modes.getBicycle())
            currentMode = TraverseMode.BICYCLE;
        else
            currentMode = null;
    }

    protected StateData clone() {
        try {
            return (StateData) super.clone();
        } catch (CloneNotSupportedException e1) {
            throw new IllegalStateException("This is not happening");
        }
    }
}
