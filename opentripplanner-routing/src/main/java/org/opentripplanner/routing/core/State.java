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

package org.opentripplanner.routing.core;

import java.util.Date;

import org.onebusaway.gtfs.model.AgencyAndId;

public class State {

    public long time;

    private int trip = -1;

    public AgencyAndId tripId = null;

    public double walkDistance = 0;

    public String zone = null;

    public AgencyAndId route = null;

    public FareContext fareContext;

    public int numBoardings = 0;

    public boolean alightedLocal = false;

    public boolean everBoarded = false;

    public Vertex previousStop = null;

    public long lastAlightedTime;

    public State() {
        this(System.currentTimeMillis());
    }

    public State(long time) {
        this.time = time;
    }

    public State(long time, int pattern, AgencyAndId tripId, double walkDistance) {
        this(time, pattern, tripId, walkDistance, null, null, null, 0, false, false, null, 0);
    }

    public State(long time, int trip, AgencyAndId tripId, double walkDistance, AgencyAndId route,
            String zone, FareContext fareContext, int numBoardings, boolean alightedLocal,
            boolean everBoarded, Vertex previousStop, long lastAlighted) {
        this.time = time;
        this.trip = trip;
        this.tripId = tripId;
        this.walkDistance = walkDistance;
        this.route = route;
        this.zone = zone;
        this.fareContext = fareContext;
        this.numBoardings = numBoardings;
        this.alightedLocal = alightedLocal;
        this.everBoarded = everBoarded;
        this.previousStop = previousStop;
        this.lastAlightedTime = lastAlighted;
    }

    public void setZoneAndRoute(String zone, AgencyAndId route, FareContext context) {
        this.zone = zone;
        this.route = route;
        fareContext = context;
    }

    /**
     * @return the time in milliseconds since the epoch.
     */
    public long getTime() {
        return time;
    }

    public void incrementTimeInSeconds(int numOfSeconds) {
        time += numOfSeconds * 1000;
    }

    public State clone() {
        State ret = new State(time, trip, tripId, walkDistance, route, zone, fareContext,
                numBoardings, alightedLocal, everBoarded, previousStop, lastAlightedTime);
        return ret;
    }

    public String toString() {
        return "<State " + new Date(time) + "," + trip + ">";
    }

    public void setPattern(int curPattern) {
        this.trip = curPattern;
    }

    public int getTrip() {
        return trip;
    }
}