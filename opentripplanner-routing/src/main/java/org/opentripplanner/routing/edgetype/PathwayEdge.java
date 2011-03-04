package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

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

/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends AbstractEdge {

    private static GeometryFactory _geometryFactory = new GeometryFactory();

    private int traversalTime;

    private int wheelchairTraversalTime = -1;

    public PathwayEdge(Vertex fromv, Vertex tov, int traversalTime, int wheelchairTraversalTime) {
        super(fromv, tov);
        this.traversalTime = traversalTime;
        this.wheelchairTraversalTime = wheelchairTraversalTime;
    }

    public PathwayEdge(Vertex fromv, Vertex tov, int traversalTime) {
        super(fromv, tov);
        this.traversalTime = traversalTime;
    }

    private static final long serialVersionUID = -3311099256178798981L;

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { getFromVertex().getCoordinate(),
                getToVertex().getCoordinate() };
        return _geometryFactory.createLineString(coordinates);
    }

    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    public String getName() {
        return "pathway";
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
        int time = traversalTime;
        if (wo.wheelchairAccessible) {
            if (wheelchairTraversalTime < 0) {
                return null;
            }
            time = wheelchairTraversalTime;
        }
        State s1 = s0.incrementTimeInSeconds(time);
        return new TraverseResult(time, s1, this);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        int time = traversalTime;
        if (wo.wheelchairAccessible) {
            if (wheelchairTraversalTime < 0) {
                return null;
            }
            time = wheelchairTraversalTime;            
        }
        State s1 = s0.incrementTimeInSeconds(-time);
        return new TraverseResult(time, s1, this);
    }
}
