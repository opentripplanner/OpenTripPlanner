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

package org.opentripplanner.routing.flex;

import com.vividsolutions.jts.geom.Geometry;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Area;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.State;

import java.io.Serializable;

public class DemandResponseService implements Serializable {

    private static final long serialVersionUID = 1L;

    private AgencyAndId serviceId;
    private Route route;
    private int startTime; // start time, in seconds past midnight
    private int endTime; // end time, in seconds past midnight
    private Geometry coverageArea;

    public DemandResponseService(Trip trip, StopTime start, StopTime end) {
        this.serviceId = trip.getServiceId();
        this.route = trip.getRoute();
        this.startTime = start.getDepartureTime();
        this.endTime = end.getArrivalTime();
        if (start.getStartServiceArea() != null) {
            coverageArea = GeometryUtils.parseWkt(start.getStartServiceArea().getWkt());
        }
    }

    public AgencyAndId getServiceId() {
        return serviceId;
    }

    public Route getRoute() {
        return route;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public Geometry getCoverageArea() {
        return coverageArea;
    }

    public boolean isApplicableTo(State s) {
        Geometry point = GeometryUtils.getGeometryFactory().createPoint(s.getVertex().getCoordinate());
        if (!getCoverageArea().contains(point))
            return false;
        int time = s.getServiceDay().secondsSinceMidnight(s.getTimeSeconds());
        return time > startTime && time < endTime;
    }

}
