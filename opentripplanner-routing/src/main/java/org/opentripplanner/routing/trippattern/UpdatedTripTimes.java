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

package org.opentripplanner.routing.trippattern;

/**
 * An UpdatedTripTimes applies updated arrival and departure times to a subset of a trip's stops,
 * reading through to the scheduled TripTimes for subsequent stops, and reporting that the vehicle
 * has passed stops preceding the update block.
 */
public class UpdatedTripTimes extends DelegatingTripTimes {

    private final int offset;
    
    private int[] arrivals;
    
    private int[] departures;
    
    // maybe push pattern and offset into block
    public UpdatedTripTimes(ScheduledTripTimes sched, UpdateBlock block, int offset) {
        super(sched);
        this.offset = offset;
        int nUpdates = block.updates.size();
        this.arrivals = new int[nUpdates];
        this.departures = new int[nUpdates];
        int ui = 0;
        for (Update update : block.updates) {
            arrivals[ui] = update.arrive;
            departures[ui] = update.depart;
            ui += 1;
        }
        this.compact();
    }

    @Override public int getDepartureTime(int hop) {
        int stop = hop;
        int update = stop - offset;
        if (update < 0)
            return TripTimes.PASSED;
        if (update >= departures.length)
            return super.getDepartureTime(hop);
        return departures[update];
    }
    
    @Override public int getArrivalTime(int hop) {
        int stop = hop + 1;
        int update = stop - offset;
        if (update < 0)
            return TripTimes.PASSED;
        if (update >= departures.length)
            return super.getArrivalTime(hop);
        if (arrivals == null)
            return departures[update];
        return arrivals[update];
    }
    
    @Override public boolean compact() {
        if (arrivals == null)
            return false;
        for (int i = 0; i < arrivals.length; i++) {
            if (arrivals[i] != departures[i]) {
                return false;
            }
        }
        arrivals = null;
        return true;
    }
    
    public String toString() {
        String s = String.format("UpdatedTripTimes block size %d at stop %d\n", 
                departures.length, offset);
        return s + dumpTimes() + "\nbased on:" + super.toString();
    }

}
