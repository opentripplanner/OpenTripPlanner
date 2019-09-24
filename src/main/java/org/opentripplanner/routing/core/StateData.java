package org.opentripplanner.routing.core;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * StateData contains the components of search state that are unlikely to be changed as often as
 * time or weight. This avoids frequent duplication, which should have a positive impact on both
 * time and space use during searches.
 */
public class StateData implements Cloneable {

    // the time at which the search started
    protected long startTime;

    // which trip index inside a pattern
    protected TripTimes tripTimes;

    protected FeedScopedId tripId;
    
    protected Trip previousTrip;

    protected double lastTransitWalk = 0;

    protected String zone;

    protected FeedScopedId route;

    protected int numBoardings;

    protected boolean everBoarded;

    protected boolean usingRentedBike;

    protected boolean usingRentedCar;

    protected boolean hasRentedCarPostTransit = false;
    protected boolean hasRentedCarPreTransit = false;

    protected boolean usingRentedVehicle;

    protected boolean hasRentedVehiclePostTransit = false;
    protected boolean hasRentedVehiclePreTransit = false;

    public boolean rentedVehicleAllowsFloatingDropoffs;

    protected boolean usingHailedCar;

    protected boolean hasHailedCarPostTransit = false;
    protected boolean hasHailedCarPreTransit = false;

    protected boolean carParked;

    protected boolean bikeParked;

    protected Stop previousStop;

    protected long lastAlightedTime;

    protected FeedScopedId[] routeSequence;

    protected HashMap<Object, Object> extensions;

    protected RoutingRequest opt;

    protected TripPattern lastPattern;

    protected ServiceDay serviceDay;

    protected TraverseMode nonTransitMode;

    /** 
     * This is the wait time at the beginning of the trip (or at the end of the trip for
     * reverse searches). In Analyst anyhow, this is is subtracted from total trip length of each
     * final State in lieu of reverse optimization. It is initially set to zero so that it will be
     * ineffectual on a search that does not ever board a transit vehicle.
     */
    protected long initialWaitTime = 0;

    /**
     * This is the time between the trip that was taken at the previous stop and the next trip
     * that could have been taken. It is used to determine if a path needs reverse-optimization.
     */
    protected int lastNextArrivalDelta;

    /**
     * The mode that was used to traverse the backEdge
     */
    protected TraverseMode backMode;

    protected boolean backWalkingBike;

    public Set<String> bikeRentalNetworks;

    public Set<String> carRentalNetworks;

    // A list of possible vehicle rental networks that the state can be associated with. This data structure is a set
    // because in an arrive-by search, the search progresses backwards from a street edge where potentially multiple
    // vehicle rental providers allow floating drop-offs at the edge.
    public Set<String> vehicleRentalNetworks;

    // The ids of cars that have been rented so far
    protected Set<String> rentedCars = new HashSet<>();

    // The ids of vehicles that have been rented so far
    protected Set<String> rentedVehicles = new HashSet<>();

    // whether the currently rented car can be dropped off anywhere inside a car rental region
    protected boolean rentedCarAllowsFloatingDropoffs;

    /* This boolean is set to true upon transition from a normal street to a no-through-traffic street. */
    protected boolean enteredNoThroughTrafficArea;

    public StateData(RoutingRequest options) {
        TraverseModeSet modes = options.modes;
        if (modes.getCar())
            nonTransitMode = TraverseMode.CAR;
        else if (modes.getWalk())
            nonTransitMode = TraverseMode.WALK;
        else if (modes.getBicycle())
            nonTransitMode = TraverseMode.BICYCLE;
        else if (modes.getMicromobility())
            nonTransitMode = TraverseMode.MICROMOBILITY;
        else
            nonTransitMode = null;
    }

    protected StateData clone() {
        try {
            StateData clonedStateData = (StateData) super.clone();
            // HashSets do not get cloned quite right, so if any HashSets exists with data that gets added to them
            // throughout a shortest path search, they must be recreated with the previous state's data in order to make
            // sure they don't contain data from other paths.
            clonedStateData.rentedCars = new HashSet<>(this.rentedCars);
            clonedStateData.rentedVehicles = new HashSet<>(this.rentedVehicles);
            return clonedStateData;
        } catch (CloneNotSupportedException e1) {
            throw new IllegalStateException("This is not happening");
        }
    }

    public int getNumBooardings(){
        return numBoardings;
    }

    public boolean hasHailedCarPostTransit() { return hasHailedCarPostTransit; }

    public boolean hasHailedCarPreTransit() { return hasHailedCarPreTransit; }

    public boolean hasRentedCarPostTransit() { return hasRentedCarPostTransit; }

    public boolean hasRentedCarPreTransit() { return hasRentedCarPreTransit; }

    public boolean rentedCarAllowsFloatingDropoffs() { return rentedCarAllowsFloatingDropoffs; }

    public Set<String> getRentedCars() { return rentedCars; }

    public Set<String> getRentedVehicles() { return rentedVehicles; }

    public boolean hasRentedVehiclePostTransit() { return hasRentedVehiclePostTransit; }

    public boolean hasRentedVehiclePreTransit() { return hasRentedVehiclePreTransit; }

    public boolean rentedVehicleAllowsFloatingDropoffs() { return rentedVehicleAllowsFloatingDropoffs; }
}
