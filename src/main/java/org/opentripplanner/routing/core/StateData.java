/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.util.HashMap;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

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

    protected AgencyAndId tripId;
    
    protected Trip previousTrip;

    protected double lastTransitWalk = 0;

    protected String zone;

    protected AgencyAndId route;

    protected int numBoardings;

    protected boolean everBoarded;

    protected boolean usingRentedBike;

    protected boolean carParked;

    protected boolean bikeParked;
    
    protected Stop previousStop;

    protected long lastAlightedTime;

    protected AgencyAndId[] routeSequence;

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
