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
import org.opentripplanner.routing.core.FareContext;
import org.opentripplanner.routing.core.OptimizeType;
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
 * Models alighting from a vehicle - that is to say, traveling from a station on vehicle to a
 * station off vehicle. When traversed backwards, the the resultant state has the time of the
 * previous arrival, in addition the pattern that was boarded. When traversed forwards, the
 * result state is unchanged. An boarding penalty can also be applied to discourage transfers.
 */
public class Alight extends AbstractEdge implements OnBoardReverseEdge {

    public Hop hop;

    private boolean wheelchairAccessible;

    private Trip trip;

    private String zone;

    private FareContext fareContext;

    private static final long serialVersionUID = 1L;

    public Alight(Vertex fromv, Vertex tov, Hop hop, boolean wheelchairAccessible, String zone, Trip trip, FareContext fareContext) {
        super(fromv, tov);
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
        return new TraverseResult(1, s0,this);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions options) {
        if (!options.getModes().contains(hop.getMode())) {
            return null;
        }
        if (options.wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        
        long current_time = state0.getTime();
        long transfer_penalty = 0;
        StateData data = state0.getData();
        
        /* apply transfer rules */
        /* look in the global transfer table for the rules from the previous stop to
         * this stop. 
         */
        if (data.getLastAlightedTime() != 0) { /* this is a transfer rather than an initial boarding */
            TransferTable transferTable = options.getTransferTable();
            
            if (transferTable.hasPreferredTransfers()) {
                transfer_penalty = options.baseTransferPenalty;
            }
            
            int transfer_time = transferTable.getTransferTime(getToVertex(), data.getPreviousStop());
            if (transfer_time == TransferTable.UNKNOWN_TRANSFER) {
                transfer_time = options.minTransferTime;
            }
            if (transfer_time > 0 && transfer_time > (current_time + data.getLastAlightedTime()) * 1000) {
                /* minimum time transfers */
                current_time += data.getLastAlightedTime() - transfer_time * 1000;
            } else if (transfer_time == TransferTable.FORBIDDEN_TRANSFER) {
                return null;
            } else if (transfer_time == TransferTable.PREFERRED_TRANSFER) {
                /* depenalize preferred transfers */
                transfer_penalty = 0; 
            }
        }

        Date serviceDate = getServiceDate(current_time, true);
        int secondsSinceMidnight = (int) ((current_time - serviceDate.getTime()) / 1000);

        CalendarService service = options.getCalendarService();
        if (!service.getServiceDatesForServiceId(hop.getServiceId()).contains(serviceDate))
            return null;

        int wait = secondsSinceMidnight - hop.getEndStopTime().getArrivalTime();
        if (wait < 0) {
            return null;
        }

        if (options.optimizeFor == OptimizeType.TRANSFERS && state0.getData().getTrip() != -1) {
            //this is not the first boarding, therefore we must have "transferred" -- whether
            //via a formal transfer or by walking.
            transfer_penalty += options.optimizeTransferPenalty;
        }

        Editor editor = state0.edit();
        editor.incrementTimeInSeconds(wait);
        editor.incrementNumBoardings();
        editor.setTripId(trip.getId());
        editor.setZone(zone);
        editor.setRoute(trip.getRoute().getId());
        editor.setFareContext(fareContext);

        State state1 = state0.incrementTimeInSeconds(-wait);
        return new TraverseResult(wait + options.boardCost + transfer_penalty, state1, this);
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
