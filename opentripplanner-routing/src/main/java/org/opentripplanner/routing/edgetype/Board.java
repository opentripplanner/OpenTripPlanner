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

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Models boarding a vehicle - that is to say, traveling from a station off vehicle to a station
 * on vehicle. When traversed forward, the the resultant state has the time of the next
 * departure, in addition the pattern that was boarded. When traversed backward, the result
 * state is unchanged.
 */
public class Board extends AbstractEdge implements OnBoardForwardEdge {

    String start_id; // a street vertex's id

    String end_id; // a transit node's GTFS id

    public Hop hop;

    private boolean wheelchairAccessible;

    public static final int SECS_IN_DAY = 86400;

    private static final long serialVersionUID = 2L;

    public Board(Vertex startStation, Vertex startJourney, Hop hop, boolean wheelchairAccessible) {
        super(startStation, startJourney);
        this.hop = hop;
	this.wheelchairAccessible = wheelchairAccessible;
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public Geometry getGeometry() {
        return null;
    }

    public TraverseMode getMode() {
        return TraverseMode.BOARDING;
    }

    public String getName() {
        return "board vehicle";
    }

    public Trip getTrip() {
        return hop.getTrip();
    }

    public TraverseResult traverse(State state0, TraverseOptions wo) {
        if (!wo.modes.contains(hop.getMode())) {
            return null;
        }

	if (wo.wheelchairAccessible && !wheelchairAccessible) {
	    return null;
	}

        long currentTime = state0.getTime();
        Date serviceDate = getServiceDate(currentTime, false);
        int secondsSinceMidnight = (int) ((currentTime - serviceDate.getTime()) / 1000);

        CalendarService service = wo.getCalendarService();
        Set<ServiceDate> serviceDates = service.getServiceDatesForServiceId(hop.getServiceId());
        if (!serviceDates.contains(serviceDate))
            return null;

        int wait = hop.getStartStopTime().getDepartureTime() - secondsSinceMidnight;
        if (wait < 0) {
            return null;
        }

        State state1 = state0.clone();
        state1.incrementTimeInSeconds(-wait);
        return new TraverseResult(wait, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
	if (wo.wheelchairAccessible && !wheelchairAccessible) {
	    return null;
	}
        State s1 = state0.clone();
        return new TraverseResult(1, s1);
    }

    private Date getServiceDate(long currentTime, boolean useArrival) {
        int scheduleTime = useArrival ? hop.getEndStopTime().getArrivalTime() : hop
                .getStartStopTime().getDepartureTime();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currentTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int dayOverflow = scheduleTime / SECS_IN_DAY;
        c.add(Calendar.DAY_OF_YEAR, -dayOverflow);
        return c.getTime();
    }
}
