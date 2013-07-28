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

import lombok.Getter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class Update extends AbstractUpdate implements Comparable<Update> {

    @Getter
    public final AgencyAndId stopId;

    @Getter
    public final Integer stopSeq;
    
    @Getter
    public final Integer delay;

    @Getter
    public int arrive; // sec since midnight

    @Getter
    public final int depart; // sec since midnight

    @Getter
    public final Status status;

    public Update (AgencyAndId tripId, AgencyAndId stopId, Integer stopSeq, int arrive, int depart, 
            Status status, long timestamp, ServiceDate serviceDate) {
        super(tripId, timestamp, serviceDate);
        this.stopId = stopId;
        this.stopSeq = stopSeq;
        this.arrive = arrive;
        this.depart = depart;
        this.status = status;
        this.delay = null;
    }


    public Update (AgencyAndId tripId, AgencyAndId stopId, Integer stopSeq, int delay, 
            Status status, long timestamp, ServiceDate serviceDate) {
        super(tripId, timestamp, serviceDate);
        this.stopId = stopId;
        this.stopSeq = stopSeq;
        this.arrive = 0;
        this.depart = 0;
        this.delay = delay;
        this.status = status;
    }
    
    public boolean hasStopSequence() {
        return stopSeq != null;
    }
    
    public boolean hasDelay() {
        return delay != null;
    }

    /**
     * This ordering is useful for breaking lists of mixed-trip updates into single-trip blocks.
     * We sort on (tripId, timestamp, serviceDate, stopSequence, depart) because there may be duplicate stops in 
     * an update list, and we want them to be in a predictable order for filtering. Usually 
     * duplicate stops are due to multiple updates for the same trip in the same message. In this 
     * case the two updates will have different timestamps, and we want to apply them in order.
     */
    @Override
    public int compareTo(Update other) {
        int result;
        result = this.tripId.compareTo(other.tripId);
        if (result != 0)
            return result;
        result = (int) (this.timestamp - other.timestamp);
        if (result != 0)
            return result;
        result = serviceDate.compareTo(other.serviceDate);
        if(result != 0)
            return result;
        result = this.stopSeq - other.stopSeq;
        if (result != 0)
            return result;
        result = this.depart - other.depart;
        return result;
    }

    @Override
    public String toString() {
        if(hasDelay())
            return String.format("Update trip %s Stop #%d:%s (%s) delay %s", 
                    tripId, stopSeq, stopId, status, delay);
        
        return String.format("Update trip %s Stop #%d:%s (%s) A%s D%s", 
                tripId, stopSeq, stopId, status, arrive, depart);
    }
    
    public static enum Status {
        PASSED,     // the vehicle has passed this stop, no point trying to board there.
        ARRIVED,    // the vehicle is at this stop. you could still board right now.
        PREDICTION, // the vehicle is expected to arrive/depart at the indicated times.
        CANCEL,     // the vehicle will not pick up or drop off passengers at this stop.
        PLANNED,    // trip is scheduled to be happening now, but is not broadcasting predictions
        UNKNOWN     // vehicle be broadcasting predictions according to the schedule but it is not
                    // also used if the bus is too far from the planned route (shapes.txt)
    }
    
}