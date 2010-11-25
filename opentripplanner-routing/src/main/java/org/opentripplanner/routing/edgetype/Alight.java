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

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Models alighting from a vehicle - that is to say, traveling from a station on vehicle to a
 * station off vehicle. When traversed backwards, the the resultant state has the time of the
 * previous arrival, in addition the pattern that was boarded. When traversed forwards, the
 * result state is unchanged. An boarding penalty can also be applied to discourage transfers.
 */
public class Alight extends AbstractEdge implements OnBoardReverseEdge {

    public Hop hop;

    private boolean wheelchairAccessible;

    private static final long serialVersionUID = 1L;

    public Alight(Vertex fromv, Vertex tov, Hop hop, boolean wheelchairAccessible) {
        super(fromv, tov);
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
        return TraverseMode.ALIGHTING;
    }

    public String getName() {
        // this text won't be used -- the streetTransitLink or StationEntrance's text will
        return "alight from vehicle";
    }

    public Trip getTrip() {
        return hop.getTrip();
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {
	if (wo.wheelchairAccessible && !wheelchairAccessible) {
	    return null;
	}
        State s1 = s0.clone();
        return new TraverseResult(1, s1,this);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions wo) {
        if (!wo.getModes().contains(hop.getMode())) {
            return null;
        }
	if (wo.wheelchairAccessible && !wheelchairAccessible) {
	    return null;
	}
        long currentTime = s0.getTime();
        Date serviceDate = getServiceDate(currentTime, true);
        int secondsSinceMidnight = (int) ((currentTime - serviceDate.getTime()) / 1000);

        CalendarService service = wo.getCalendarService();
        if (!service.getServiceDatesForServiceId(hop.getServiceId()).contains(serviceDate))
            return null;

        int wait = secondsSinceMidnight - hop.getEndStopTime().getArrivalTime();
        if (wait < 0) {
            return null;
        }

        State state1 = s0.clone();
        state1.incrementTimeInSeconds(-wait);
        return new TraverseResult(wait, state1,this);
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

        int dayOverflow = scheduleTime / Board.SECS_IN_DAY;
        c.add(Calendar.DAY_OF_YEAR, -dayOverflow);
        return c.getTime();
    }
}
