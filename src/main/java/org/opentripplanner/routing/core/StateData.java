package org.opentripplanner.routing.core;

import org.opentripplanner.routing.api.request.RoutingRequest;

import java.util.Set;

/**
 * StateData contains the components of search state that are unlikely to be changed as often as
 * time or weight. This avoids frequent duplication, which should have a positive impact on both
 * time and space use during searches.
 */
public class StateData implements Cloneable {

    // the time at which the search started
    protected long startTime;

    // TODO OTP2 Many of these could be replaced by a more generic state machine implementation

    protected boolean usingRentedBike;

    protected boolean carParked;

    protected boolean bikeParked;

    protected boolean hasUsedRentedBike;

    protected CarPickupState carPickupState;

    protected RoutingRequest opt;

    protected TraverseMode nonTransitMode;

    /**
     * The mode that was used to traverse the backEdge
     */
    protected TraverseMode backMode;

    protected boolean backWalkingBike;

    public Set<String> bikeRentalNetworks;

    /* This boolean is set to true upon transition from a normal street to a no-through-traffic street. */
    protected boolean enteredNoThroughTrafficArea;

    public StateData(RoutingRequest options) {
        TraverseModeSet modes = options.streetSubRequestModes;
        if (modes.getCar())
            nonTransitMode = TraverseMode.CAR;
        else if (modes.getWalk())
            nonTransitMode = TraverseMode.WALK;
        else if (modes.getBicycle())
            nonTransitMode = TraverseMode.BICYCLE;
        else
            nonTransitMode = null;
    }

    protected StateData clone() {
        try {
            return (StateData) super.clone();
        } catch (CloneNotSupportedException e1) {
            throw new IllegalStateException("This is not happening");
        }
    }
}
