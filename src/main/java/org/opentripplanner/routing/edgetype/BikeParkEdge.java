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

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.BikeParkVertex;

import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/**
 * Parking a bike edge.
 * 
 * Note: There is an edge only in the "park" direction. We do not handle (yet) unparking a bike, as
 * you would need to know where you have parked your car, and is probably better handled by the
 * client by issuing two requests (first one from your origin to your bike, second one from your
 * bike to your destination).
 * 
 * Cost is the time to park a bike, estimated.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 */
public class BikeParkEdge extends Edge {

    private static final long serialVersionUID = 1L;

    public BikeParkEdge(BikeParkVertex bikeParkVertex) {
        super(bikeParkVertex, bikeParkVertex);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        if (options.arriveBy) {
            return traverseUnpark(s0);
        } else {
            return traversePark(s0);
        }
    }

    protected State traverseUnpark(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * To unpark a bike, we need to be walking, and be allowed to bike.
         */
        if (s0.getNonTransitMode() != TraverseMode.WALK || !options.modes.getBicycle())
            return null;

        StateEditor s0e = s0.edit(this);
        s0e.incrementWeight(options.bikeParkCost);
        s0e.incrementTimeInSeconds(options.bikeParkTime);
        s0e.setBackMode(TraverseMode.LEG_SWITCH);
        s0e.setBikeParked(false);
        State s1 = s0e.makeState();
        return s1;
    }

    protected State traversePark(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * To park a bike, we need to be riding one, (not rented) and be allowed to walk and to park
         * it.
         */
        if (s0.getNonTransitMode() != TraverseMode.BICYCLE || !options.modes.getWalk()
                || s0.isBikeRenting() || s0.isBikeParked())
            return null;
        BikeParkVertex bikeParkVertex = (BikeParkVertex) tov;
        if (bikeParkVertex.getSpacesAvailable() == 0) {
            return null;
        }

        StateEditor s0e = s0.edit(this);
        s0e.incrementWeight(options.bikeParkCost);
        s0e.incrementTimeInSeconds(options.bikeParkTime);
        s0e.setBackMode(TraverseMode.LEG_SWITCH);
        s0e.setBikeParked(true);
        State s1 = s0e.makeState();
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

    public boolean equals(Object o) {
        if (o instanceof BikeParkEdge) {
            BikeParkEdge other = (BikeParkEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "BikeParkEdge(" + fromv + " -> " + tov + ")";
    }
}
