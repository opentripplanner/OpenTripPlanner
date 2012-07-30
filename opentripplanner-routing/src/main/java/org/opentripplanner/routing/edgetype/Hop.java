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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Hop extends AbstractEdge implements OnBoardForwardEdge, OnBoardReverseEdge, HopEdge {

    private static final long serialVersionUID = -7761092317912812048L;

    private StopTime start;

    private StopTime end;

    private Trip trip;

    private int _serviceId;

    private int elapsed;

    public int getServiceId() {
        return _serviceId;
    }

    public Hop(Vertex startJourney, Vertex endJourney, StopTime start, StopTime end, Trip trip, int serviceId) {
        super(startJourney, endJourney);
        this.start = start;
        this.end = end;
        this.trip = trip;
        this._serviceId = serviceId;
        this.elapsed = end.getArrivalTime() - start.getDepartureTime();
    }

    public StopTime getStartStopTime() {
        return start;
    }

    public StopTime getEndStopTime() {
        return end;
    }

    public State traverse(State s0) {
        EdgeNarrative en = new TransitNarrative(trip, start.getStopHeadsign(), this);
        StateEditor s1 = s0.edit(this, en);
        s1.incrementTimeInSeconds(elapsed);
        s1.incrementWeight(elapsed);
        if (s0.getOptions().isArriveBy())
            s1.setZone(getStartStop().getZoneId());
        else
            s1.setZone(getEndStop().getZoneId());
        //s1.setRoute(start.getTrip().getRoute().getId());
        return s1.makeState();
    }

    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options);
    }

    public double timeLowerBound(RoutingRequest options) {
        return elapsed;
    }

    public String toString() {
        return this.start + " " + this.end + " " + this._serviceId;
    }

    private Geometry geometry = null;

    public String getDirection() {
        return start.getTrip().getTripHeadsign();
    }

    public double getDistance() {
        Stop stop1 = start.getStop();
        Stop stop2 = end.getStop();
        return SphericalDistanceLibrary.getInstance().distance(stop1.getLat(), stop1.getLon(),
                stop2.getLat(), stop2.getLon());
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(start.getTrip().getRoute());
    }

    public String getName() {
        return GtfsLibrary.getRouteName(start.getTrip().getRoute());
    }

    public Boolean getBikesAllowed() {
        return (start.getTrip().getRoute().getBikesAllowed() == 2 && start.getTrip()
                .getTripBikesAllowed() != 1) || start.getTrip().getTripBikesAllowed() == 2;
    }

    public Geometry getGeometry() {
        if (geometry == null) {

            Stop stop1 = start.getStop();
            Stop stop2 = end.getStop();

            Coordinate c1 = new Coordinate(stop1.getLon(), stop1.getLat());
            Coordinate c2 = new Coordinate(stop2.getLon(), stop2.getLat());

            geometry = GeometryUtils.getGeometryFactory().createLineString(
                    new Coordinate[] { c1, c2 });
        }
        return geometry;
    }

    public void setGeometry(Geometry line) {
        geometry = line;
    }

    @Override
    public Stop getEndStop() {
        return end.getStop();
    }

    @Override
    public Stop getStartStop() {
        return start.getStop();
    }

    public Trip getTrip() {
        return start.getTrip();
    }

}
