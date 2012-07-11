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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.model.T2;

public class FrequencyBasedTripPattern implements Serializable, TripPattern {

    private static final long serialVersionUID = -5392197874345648815L;

    private Trip exemplar;

    // sorted by start time
    @XmlElement
    int[] timeRangeStart;

    @XmlElement
    int[] timeRangeEnd;

    @XmlElement
    int[] timeRangeFrequency;

    /*
     * All of the following arrays are indexed by stop sequence; the first departure time is always
     * 00:00:00
     */
    @XmlElement
    int[] departureTimes;

    int[] runningTimes;

    @XmlElement
    int[] arrivalTimes;

    int[] dwellTimes;

    private String[] headsigns;

    @XmlElement
    private String[] zones;

    @XmlElement
    private int tripFlags;

    @XmlElement
    private int[] perStopFlags;

    private transient List<Stop> stops;
    
    boolean exact;

    public FrequencyBasedTripPattern(Trip trip, int size) {
        this.exemplar = trip;
        departureTimes = new int[size];
        runningTimes = new int[size];

        arrivalTimes = new int[size];
        dwellTimes = new int[size];

        headsigns = new String[size];

        zones = new String[size];

        perStopFlags = new int[size];

    }

    public int getNextDepartureTime(int stopIndex, int afterTime, boolean wheelchairAccessible,
            boolean bikesAllowed, boolean pickup) {
        int mask = pickup ? TableTripPattern.MASK_PICKUP : TableTripPattern.MASK_DROPOFF;
        int shift = pickup ? TableTripPattern.SHIFT_PICKUP : TableTripPattern.SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex] & mask) >> shift == TableTripPattern.NO_PICKUP) {
            return -1;
        }
        if (wheelchairAccessible
                && (perStopFlags[stopIndex] & TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }

        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? TableTripPattern.FLAG_BIKES_ALLOWED : 0)
                    | (wheelchairAccessible ? TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            if ((tripFlags & flags) == 0) {
                return -1;
            }
        }

        int stopDepartureTimeOffset = departureTimes[stopIndex];
        int afterTimeAtStart = afterTime - stopDepartureTimeOffset;

        int timeRange = Arrays.binarySearch(timeRangeStart, afterTimeAtStart);

        if (timeRange < 0) {
            timeRange = -timeRange - 1;
            if (timeRange > 0)
                timeRange -= 1;
        }

        int firstDepartureTimeInRange = stopDepartureTimeOffset + timeRangeStart[timeRange];
        int frequency = timeRangeFrequency[timeRange];

        int departureTime;
        if (exact) {
            if (afterTime < firstDepartureTimeInRange) {
                departureTime = firstDepartureTimeInRange;
            } else {
                int offset = (afterTime - firstDepartureTimeInRange) % frequency;
                if (offset == 0)
                    offset = frequency; // catch exact hits
                departureTime = afterTime + frequency - offset;
            }
        } else {
            departureTime = afterTime + frequency;
            if (departureTime < firstDepartureTimeInRange) {
                departureTime = firstDepartureTimeInRange;
            }
        }

        if (departureTime - stopDepartureTimeOffset < timeRangeEnd[timeRange]) {
            return departureTime;
        }
        timeRange++;

        return -1;

    }

    public int getPreviousArrivalTime(int stopIndex, int beforeTime,
            boolean wheelchairAccessible, boolean bikesAllowed, boolean pickup) {
        int mask = pickup ? TableTripPattern.MASK_PICKUP : TableTripPattern.MASK_DROPOFF;
        int shift = pickup ? TableTripPattern.SHIFT_PICKUP : TableTripPattern.SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex + 1] & mask) >> shift == TableTripPattern.NO_PICKUP) {
            return -1;
        }
        if (wheelchairAccessible
                && (perStopFlags[stopIndex] & TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }

        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? TableTripPattern.FLAG_BIKES_ALLOWED : 0)
                    | (wheelchairAccessible ? TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            if ((tripFlags & flags) == 0) {
                return -1;
            }
        }

        int stopArrivalTimeOffset = arrivalTimes[stopIndex];
        int beforeTimeAtStart = beforeTime - stopArrivalTimeOffset;

        int timeRange = Arrays.binarySearch(timeRangeEnd, beforeTimeAtStart);

        if (timeRange < 0) {
            timeRange = -timeRange - 1;
        }
        
        if (timeRange >= timeRangeStart.length) timeRange -= 1;

        if (beforeTimeAtStart < timeRangeStart[timeRange]) timeRange -= 1;
        if (timeRange < 0) return -1;
        
        int frequency = timeRangeFrequency[timeRange];
        int frequencyOffset = (timeRangeEnd[timeRange] - timeRangeStart[timeRange]) % frequency;
        int lastArrivalTimeInRange = stopArrivalTimeOffset + timeRangeEnd[timeRange] - frequencyOffset;
        
        int arrivalTime;

        if (exact) {

            if (beforeTime > lastArrivalTimeInRange) {
                arrivalTime = lastArrivalTimeInRange;
            } else {
                int offset = (lastArrivalTimeInRange - beforeTime) % frequency;
                if (offset == 0)
                    offset = frequency; // catch exact hits
                arrivalTime = beforeTime - frequency + offset;
            }
        } else {
            arrivalTime = beforeTime - frequency;
        }

        int arrivalTimeAtFirstStop = arrivalTime - stopArrivalTimeOffset;
        if (arrivalTimeAtFirstStop < timeRangeEnd[timeRange] && arrivalTimeAtFirstStop >= timeRangeStart[timeRange]) {
            return arrivalTime;
        }
        return -1;

    }

    public int getDwellTime(int stopIndex) {
        return dwellTimes[stopIndex];
    }

    public int getRunningTime(int stopIndex) {
        return runningTimes[stopIndex];
    }

    public boolean getWheelchairAccessible(int stopIndex) {
        if ((perStopFlags[stopIndex] & TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return false;
        }
        return true;
    }

    public boolean getBikesAllowed(int trip) {
        return (tripFlags & TableTripPattern.FLAG_BIKES_ALLOWED) != 0;
    }

    public boolean canAlight(int stopIndex) {
        return getAlightType(stopIndex) != TableTripPattern.NO_PICKUP;
    }

    public boolean canBoard(int stopIndex) {
        return getBoardType(stopIndex) != TableTripPattern.NO_PICKUP;
    }

    public String getZone(int stopIndex) {
        return zones[stopIndex];
    }

    public Object getTrip(int trip) {
        return new T2<Trip, Integer>(exemplar, trip);
    }

    public String getHeadsign(int stopIndex) {
        return headsigns[stopIndex];
    }

    public int getAlightType(int stopIndex) {
        return (perStopFlags[stopIndex] & TableTripPattern.MASK_DROPOFF) >> TableTripPattern.SHIFT_DROPOFF;
    }

    public int getBoardType(int stopIndex) {
        return (perStopFlags[stopIndex] & TableTripPattern.MASK_PICKUP) >> TableTripPattern.SHIFT_PICKUP;
    }

    public void createRanges(List<Frequency> frequencies) {
        timeRangeStart = new int[frequencies.size()];
        timeRangeEnd = new int[frequencies.size()];
        timeRangeFrequency = new int[frequencies.size()];
        for (int i = 0; i < frequencies.size(); ++i) {
            Frequency frequency = frequencies.get(i);
            timeRangeStart[i] = frequency.getStartTime();
            timeRangeEnd[i] = frequency.getEndTime();
            timeRangeFrequency[i] = frequency.getHeadwaySecs();
            exact = frequency.getExactTimes() == 1; // FIXME: assumes all frequencies work the same
                                                    // way
        }
    }

    public int getTripFlags() {
        return tripFlags;
    }

    public void setTripFlags(int tripFlags) {
        this.tripFlags = tripFlags;
    }

    public Trip getTrip() {
        return exemplar;
    }

    public void addHop(int index, int departureTime, int runningTime, int arrivalTime,
            int dwellTime, String headsign) {
        departureTimes[index] = departureTime;
        runningTimes[index] = runningTime;
        arrivalTimes[index+1] = arrivalTime;
        dwellTimes[index] = dwellTime;
        headsigns[index] = headsign;

    }

    @Override
    public List<Stop> getStops() {
        return stops;
    }

    public void setStops(List<Stop> stops) {
        this.stops = stops;
    }

}
