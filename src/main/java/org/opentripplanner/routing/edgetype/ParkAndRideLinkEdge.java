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

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/**
 * This represents the connection between a P+R and the street access.
 * 
 * @author laurent
 */
public class ParkAndRideLinkEdge extends Edge {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    /*
     * By how much we have to really walk compared to straight line distance. This is a magic factor
     * as we really can't guess, unless we know 1) where the user will park, and 2) we route inside
     * the parking lot.
     *
     * TODO: perhaps all of this obstruction and distance calculation should just be reduced to
     * a single static cost. Parking lots are not that big, and these are all guesses.
     */
    private double WALK_OBSTRUCTION_FACTOR = 2.0;

    private double DRIVE_OBSTRUCTION_FACTOR = 2.0;

    /* This is magic too. Driver tend to drive slowly in P+R. */
    private double DRIVE_SPEED_MS = 3;

    private ParkAndRideVertex parkAndRideVertex;

    private boolean exit;

    @SuppressWarnings("unused")
    private LineString geometry = null;

    /** The estimated distance between the center of the P+R envelope and the street access. */
    private double linkDistance;

    public ParkAndRideLinkEdge(ParkAndRideVertex from, Vertex to) {
        super(from, to);
        parkAndRideVertex = from;
        exit = true;
        initGeometry();
    }

    public ParkAndRideLinkEdge(Vertex from, ParkAndRideVertex to) {
        super(from, to);
        parkAndRideVertex = to;
        exit = false;
        initGeometry();
    }

    private void initGeometry() {
        Coordinate fromc = fromv.getCoordinate();
        Coordinate toc = tov.getCoordinate();
        geometry = GeometryUtils.getGeometryFactory().createLineString(
                new Coordinate[] { fromc, toc });
        linkDistance = SphericalDistanceLibrary.distance(fromc, toc);
    }

    @Override
    public String getName() {
        // TODO I18n
        return parkAndRideVertex.getName() + (exit ? " (exit)" : " (entrance)");
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    @Override
    public State traverse(State s0) {
        // Do not enter park and ride mechanism if it's not activated in the routing request.
        if ( ! s0.getOptions().parkAndRide) {
            return null;
        }
        Edge backEdge = s0.getBackEdge();
        boolean back = s0.getOptions().arriveBy;
        // If we are exiting (or entering-backward), check if we
        // really parked a car: this will prevent using P+R as
        // shortcut.
        if ((back != exit) && !(backEdge instanceof ParkAndRideEdge))
            return null;

        StateEditor s1 = s0.edit(this);
        TraverseMode mode = s0.getNonTransitMode();
        if (mode == TraverseMode.WALK) {
            // Walking
            double walkTime = linkDistance * WALK_OBSTRUCTION_FACTOR
                    / s0.getOptions().walkSpeed;
            s1.incrementTimeInSeconds((int) Math.round(walkTime));
            s1.incrementWeight(walkTime);
            s1.incrementWalkDistance(linkDistance);
            s1.setBackMode(TraverseMode.WALK);
        } else if (mode == TraverseMode.CAR) {
            // Driving
            double driveTime = linkDistance * DRIVE_OBSTRUCTION_FACTOR / DRIVE_SPEED_MS;
            s1.incrementTimeInSeconds((int) Math.round(driveTime));
            s1.incrementWeight(driveTime);
            s1.setBackMode(TraverseMode.CAR);
        } else {
            // Can't cycle in/out a P+R.
            return null;
        }
        return s1.makeState();
    }

    @Override
    public State optimisticTraverse(State s0) {
        return traverse(s0);
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        boolean parkAndRide = options.modes.getWalk() && options.modes.getCar();
        return parkAndRide ? 0 : Double.POSITIVE_INFINITY;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public String toString() {
        return "ParkAndRideLinkEdge(" + fromv + " -> " + tov + ")";
    }
}
