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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

public class PatternBoard extends PatternEdge {

    /**
     * Models boarding a vehicle - that is to say, traveling from a station off vehicle to a station
     * on vehicle. When traversed forward, the the resultant state has the time of the next
     * departure, in addition the pattern that was boarded. When traversed backward, the result
     * state is unchanged. An boarding penalty can also be applied to discourage transfers.
     */

    private static final long serialVersionUID = 1042740795612978747L;

    private static final long MILLI_IN_DAY = 24 * 60 * 60 * 1000;

    private static final int SEC_IN_DAY = 24 * 60 * 60;

    private static final int BOARD_COST = 120;

    private int stopIndex;

    private int modeMask;

    public PatternBoard(Vertex startStation, Vertex startJourney, TripPattern pattern, int stopIndex, TraverseMode mode) {
        super(startStation, startJourney, pattern);
        this.stopIndex = stopIndex;
        this.modeMask = new TraverseModeSet(mode).getMask();
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
        return "leave street network for transit network";
    }

    public TraverseResult traverse(State state0, TraverseOptions options) {
        if (!options.modes.get(modeMask)) {
            return null;
        }
        long currentTime = state0.getTime();
        ServiceDate serviceDate = getServiceDate(currentTime, options.calendar);
        ServiceDate serviceDateYesterday = getServiceDate(currentTime - MILLI_IN_DAY, options.calendar);
        int secondsSinceMidnight = (int) ((currentTime - serviceDate.getAsDate().getTime()) / 1000);

        int wait = -1;
        int patternIndex = -1;
        AgencyAndId service = getPattern().getExemplar().getServiceId();
        if (options.serviceOn(service, serviceDate)) {
            // try to get the departure time on today's schedule
            patternIndex = getPattern().getNextTrip(stopIndex, secondsSinceMidnight, options.wheelchairAccessible, true);
            if (patternIndex >= 0) {
                wait = getPattern().getDepartureTime(stopIndex, patternIndex) - secondsSinceMidnight;
            }
        }
        if (options.serviceOn(service, serviceDateYesterday)) {
            // now, try to get the departure time on yesterday's schedule -- assuming that
            // yesterday's is on the same schedule as today. If it's not, then we'll worry about it
            // when we get to the pattern(s) which do contain yesterday.
            int yesterdayPatternIndex = getPattern().getNextTrip(stopIndex, secondsSinceMidnight
                    + SEC_IN_DAY, options.wheelchairAccessible, true);
            if (yesterdayPatternIndex >= 0) {
                int waitYesterday = getPattern().getDepartureTime(stopIndex, yesterdayPatternIndex)
                        - secondsSinceMidnight - SEC_IN_DAY;
                if (wait < 0 || waitYesterday < wait) {
                    // choose the better time
                    wait = waitYesterday;
                    patternIndex = yesterdayPatternIndex;
                }
            }
        }

        if (wait < 0) {
            return null;
        }
        State state1 = state0.clone();
        state1.setPattern(patternIndex);
        state1.incrementTimeInSeconds(wait);
        state1.tripId = getPattern().getTrip(patternIndex).getId();
        state1.setZoneAndRoute(getPattern().getZone(stopIndex), getPattern().getExemplar().getRoute().getId(), getPattern().getFareContext());
        long transfer_penalty = 0;
        if (options.optimizeFor == OptimizeType.TRANSFERS && state0.getTrip() != -1) {
            //this is not the first boarding, therefore we must have "transferred" -- whether
            //via a formal transfer or by walking.
            transfer_penalty = options.optimizeTransferPenalty;
        }
        return new TraverseResult(wait + BOARD_COST + transfer_penalty, state1);
    }

    public TraverseResult traverseBack(State state0, TraverseOptions wo) {
	if (!getPattern().canBoard(stopIndex)) {
            return null;
        }
        State s1 = state0.clone();
        s1.tripId = null;
        return new TraverseResult(1, s1);
    }

    private ServiceDate getServiceDate(long currentTime, Calendar c) {
        c.setTimeInMillis(currentTime);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return new ServiceDate(c.getTime());
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public String toString() {
        return "PatternBoard(" + super.toString() + ")";
    }
}
