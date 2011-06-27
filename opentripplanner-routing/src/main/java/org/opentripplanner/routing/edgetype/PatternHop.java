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

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * A transit vehicle's journey between departure at one stop and arrival at the next.
 * This version represents a set of such journeys specified by a TripPattern.
 */
public class PatternHop extends PatternEdge implements OnBoardForwardEdge, OnBoardReverseEdge {

    private static final long serialVersionUID = 1L;

    private Stop start, end;

    private int stopIndex;

    private Geometry geometry = null;

    public PatternHop(Vertex startJourney, Vertex endJourney, Stop start, Stop end, int stopIndex,
            TripPattern tripPattern) {
        super(startJourney, endJourney, tripPattern);
        this.start = start;
        this.end = end;
        this.stopIndex = stopIndex;
    }

    public String getDirection() {
        return pattern.getExemplar().getTripHeadsign();
    }

    public double getDistance() {
        return DistanceLibrary.distance(start.getLat(), start.getLon(), end.getLat(), end.getLon());
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(pattern.getExemplar().getRoute());
    }

    public String getName() {
        return GtfsLibrary.getRouteName(pattern.getExemplar().getRoute());
    }
    
    public State optimisticTraverse(State state0) {
    	int runningTime = pattern.getBestRunningTime(stopIndex);
    	StateEditor s1 = state0.edit(this);
    	s1.incrementTimeInSeconds(runningTime);
    	s1.incrementWeight(runningTime);
    	return s1.makeState();
    }

    public State optimisticTraverseBack(State state0) {
        int runningTime = pattern.getBestRunningTime(stopIndex);
        StateEditor s1 = state0.edit(this);
        s1.incrementTimeInSeconds(-runningTime);
        s1.incrementWeight(runningTime);
        return s1.makeState();
    }
    
    public State traverse(State state0) {
        int trip = state0.getTrip();
        int runningTime = pattern.getRunningTime(stopIndex, trip);
        EdgeNarrative en = new RouteNameNarrative(getPattern().getTrip(trip), this);
        StateEditor s1 = state0.edit(this, en);
        s1.incrementTimeInSeconds(runningTime);
        s1.setZone(getEndStop().getZoneId());
        s1.setRoute(pattern.getExemplar().getRoute().getId());
        s1.incrementWeight(runningTime);
        return s1.makeState();
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Geometry getGeometry() {
        if (geometry == null) {
            GeometryFactory factory = new GeometryFactory(new PrecisionModel(
                    PrecisionModel.FLOATING), 4326);

            Coordinate c1 = new Coordinate(start.getLon(), start.getLat());
            Coordinate c2 = new Coordinate(end.getLon(), end.getLat());

            geometry = factory.createLineString(new Coordinate[] { c1, c2 });
        }
        return geometry;
    }

    public Stop getEndStop() {
        return end;
    }

    public Stop getStartStop() {
        return start;
    }

    public String toString() {
    	return "PatternHop(" + getFromVertex() + ", " + getToVertex() + ")";
    }
}
