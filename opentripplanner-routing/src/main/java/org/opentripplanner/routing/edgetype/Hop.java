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

import java.util.ArrayList;
import java.util.Comparator;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.FareContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class Hop extends AbstractEdge implements Comparable<Hop> {

    public static class HopArrivalTimeComparator implements Comparator<Hop> {

        public int compare(Hop arg0, Hop arg1) {
            int v1 = arg0.end.getArrivalTime();
            int v2 = arg1.end.getArrivalTime();
            return v1 - v2;
        }

    }

    private static final long serialVersionUID = -7761092317912812048L;

    private StopTime start;

    private StopTime end;

    private AgencyAndId _serviceId;

    private int elapsed;
    
    public AgencyAndId getServiceId() {
        return _serviceId;
    }

    public Hop(Vertex startJourney, Vertex endJourney, StopTime start, StopTime end) {
        super(startJourney, endJourney);
        this.start = start;
        this.end = end;
        this._serviceId = start.getTrip().getServiceId();
        this.elapsed = end.getArrivalTime() - start.getDepartureTime();
    }

    public StopTime getStartStopTime() {
        return start;
    }

    public StopTime getEndStopTime() {
        return end;
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(elapsed);
        state1.setZoneAndRoute(getEndStop().getZoneId(), start.getTrip().getRoute().getId(), fareContext);
        return new TraverseResult(elapsed, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
        State state1 = state0.clone();
        state1.incrementTimeInSeconds(-elapsed);
        state1.setZoneAndRoute(getStartStop().getZoneId(), start.getTrip().getRoute().getId(), fareContext);
        return new TraverseResult(elapsed, state1);
    }

    public int compareTo(Hop arg0) {
        return this.end.compareTo(arg0.end);
    }

    public String toString() {
        return this.start + " " + this.end + " " + this._serviceId;
    }

    private Geometry geometry = null;

    private FareContext fareContext;

    public String getDirection() {
        return start.getTrip().getTripHeadsign();
    }

    public double getDistance() {
        Stop stop1 = start.getStop();
        Stop stop2 = end.getStop();
        return DistanceLibrary.distance(stop1.getLat(), stop1.getLon(), stop2.getLat(), stop2.getLon());
    }

    public TraverseMode getMode() {
        return GtfsLibrary.getTraverseMode(start.getTrip().getRoute());
    }

    public String getName() {
        return GtfsLibrary.getRouteName(start.getTrip().getRoute());
    }

    public Geometry getGeometry() {
        if (geometry == null) {

            GeometryFactory factory = new GeometryFactory(new PrecisionModel(
                    PrecisionModel.FLOATING), 4326);
            Stop stop1 = start.getStop();
            Stop stop2 = end.getStop();

            Coordinate c1 = new Coordinate(stop1.getLon(), stop1.getLat());
            Coordinate c2 = new Coordinate(stop2.getLon(), stop2.getLat());

            geometry = factory.createLineString(new Coordinate[] { c1, c2 });
        }
        return geometry;
    }

    public void setGeometry(Geometry line) {
        geometry = line;
    }

    public Stop getEndStop() {
       return end.getStop();
    }

    public Stop getStartStop() {
        return start.getStop();
    }

    public Trip getTrip() {
        return start.getTrip();
    }

    public void setFareContext(FareContext context) {
        fareContext = context;
    }
}
