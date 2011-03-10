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
import org.opentripplanner.routing.core.FareContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.StateData.Editor;

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

    private String zone;

    private FareContext fareContext;

    private Trip trip;

    public static final int SECS_IN_DAY = 86400;

    private static final long serialVersionUID = 2L;

    public Board(Vertex startStation, Vertex startJourney, Hop hop, boolean wheelchairAccessible, String zone, Trip trip, FareContext fareContext) {
        super(startStation, startJourney);
        this.hop = hop;
	this.wheelchairAccessible = wheelchairAccessible;
	this.zone = zone;
	this.trip = trip;
	this.fareContext = fareContext;
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

    public TraverseResult traverse(State state0, TraverseOptions options) {
        if (!options.getModes().contains(hop.getMode())) {
            return null;
        }

	if (options.wheelchairAccessible && !wheelchairAccessible) {
	    return null;
	}

        long current_time = state0.getTime();
        long transfer_penalty = 0;

        StateData data = state0.getData();
        if (data.getLastAlightedTime() != 0) { /* this is a transfer rather than an initial boarding */
            TransferTable transferTable = options.getTransferTable();
            
            if (transferTable.hasPreferredTransfers()) {
                transfer_penalty = options.baseTransferPenalty;
            }
            
            int transfer_time = transferTable.getTransferTime(data.getPreviousStop(), getFromVertex());
            if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
                transfer_time = options.minTransferTime;
            }
            if (transfer_time > 0 && transfer_time > (current_time - data.getLastAlightedTime()) * 1000) {
                /* minimum time transfers */
                current_time += data.getLastAlightedTime() + transfer_time * 1000;
            } else if (transfer_time == TransferTable.FORBIDDEN_TRANSFER) {
                return null;
            } else if (transfer_time == TransferTable.PREFERRED_TRANSFER) {
                /* depenalize preferred transfers */
                transfer_penalty = 0; 
            }
        }
	
	Date serviceDate = getServiceDate(current_time, false);
        int secondsSinceMidnight = (int) ((current_time - serviceDate.getTime()) / 1000);

        CalendarService service = options.getCalendarService();
        Set<ServiceDate> serviceDates = service.getServiceDatesForServiceId(hop.getServiceId());
        if (!serviceDates.contains(serviceDate))
            return null;

        int wait = hop.getStartStopTime().getDepartureTime() - secondsSinceMidnight;
        if (wait < 0) {
            return null;
        }

        Editor editor = state0.edit();
        editor.incrementTimeInSeconds(wait);
        editor.incrementNumBoardings();
        editor.setTripId(trip.getId());
        editor.setZone(zone);
        editor.setRoute(trip.getRoute().getId());
        editor.setFareContext(fareContext);

        return new TraverseResult(wait + options.boardCost + transfer_penalty, editor.createState(), this);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
	if (wo.wheelchairAccessible && !wheelchairAccessible) {
	    return null;
	}
        return new TraverseResult(1, state0, this);
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
