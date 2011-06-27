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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.factory.TripOvertakingException;

/**
 * A simple implementation of TripPattern that's convenient for graph building, but slow and memory-hogging.
 */
public final class BasicTripPattern implements Serializable, TripPattern {

    private static final long serialVersionUID = 1L;

    public Trip exemplar;

    /** This field, and the following three fields, are one arraylist of departure times for each stop 
     */
    private ArrayList<Integer>[] departureTimes;

    private ArrayList<Integer>[] runningTimes;

    private ArrayList<Integer>[] arrivalTimes;

    private ArrayList<Integer>[] dwellTimes;
    
    private String[] zones;

    private ArrayList<Integer> perTripFlags;
    private int[] perStopFlags;

    private ArrayList<Trip> trips;

    public ArrayTripPattern arrayPattern = null;

    public ArrayList<Stop> stops;
    
    @SuppressWarnings("unchecked")
    public BasicTripPattern(Trip exemplar, List<StopTime> stopTimes) {
        this.exemplar = exemplar;
        int hops = stopTimes.size() - 1;
        departureTimes = (ArrayList<Integer>[]) Array.newInstance(ArrayList.class, hops);
        runningTimes = (ArrayList<Integer>[]) Array.newInstance(ArrayList.class, hops);
        dwellTimes = (ArrayList<Integer>[]) Array.newInstance(ArrayList.class, hops);
        arrivalTimes = (ArrayList<Integer>[]) Array.newInstance(ArrayList.class, hops);
        perTripFlags = new ArrayList<Integer>();
        perStopFlags = new int[hops + 1];
        zones = new String[hops + 1];
        trips = new ArrayList<Trip>();
        int i;
        for (i = 0; i < hops; ++i) {
            departureTimes[i] = new ArrayList<Integer>();
            runningTimes[i] = new ArrayList<Integer>();
            dwellTimes[i] = new ArrayList<Integer>();
            arrivalTimes[i] = new ArrayList<Integer>();
        }

        stops = new ArrayList<Stop>(stopTimes.size());
        i = 0;
        for (StopTime stopTime : stopTimes) {
            stops.add(stopTime.getStop());
            zones[i] = stopTimes.get(i).getStop().getZoneId();
            if (stopTime.getStop().getWheelchairBoarding() != 0) {
                perStopFlags[i] |= FLAG_WHEELCHAIR_ACCESSIBLE;
            }
            if (stopTime.getPickupType() != 1) {
                perStopFlags[i] |= FLAG_PICKUP;
            }
            if (stopTime.getDropOffType() != 1) {
                perStopFlags[i] |= FLAG_DROPOFF;
            }
            ++i;
        }
    }

    /**
     * Remove a stop from a given trip.  This is useful when, while adding hops iteratively,
     * it turns out that the trip is an overtaking trip.
     */
    public void removeHop(int stopIndex, int hop) {
        runningTimes[stopIndex].remove(hop);
        departureTimes[stopIndex].remove(hop);
        dwellTimes[stopIndex].remove(hop);
        arrivalTimes[stopIndex].remove(hop);
        if (stopIndex == 0) {
            perTripFlags.remove(hop);
            trips.remove(hop);
        }
    }

    public void setTripFlags(int trip, int flags) {
        perTripFlags.set(trip, flags);
    }

    public void setStopFlags(int trip, int flags) {
        perStopFlags[trip] = flags;
    }

    /** 
     * Insert a hop at the correct point in the list of hops for a given trip.
     * @return 
     */
    public void addHop(int stopIndex, int insertionPoint, int departureTime, int runningTime,
            int arrivalTime, int dwellTime, Trip trip) {
        ArrayList<Integer> stopRunningTimes = runningTimes[stopIndex];
        ArrayList<Integer> stopDepartureTimes = departureTimes[stopIndex];
        ArrayList<Integer> stopArrivalTimes = arrivalTimes[stopIndex];
        ArrayList<Integer> stopDwellTimes = dwellTimes[stopIndex];

        // throw an exception when this departure time is not between the departure times it
        // should be between, indicating a trip that overtakes another.

        if (insertionPoint > 0) {
            if (stopDepartureTimes.get(insertionPoint - 1) > departureTime) {
                throw new TripOvertakingException(trip, trips.get(insertionPoint - 1), stopIndex);
            }
        }
        if (insertionPoint < stopDepartureTimes.size()) {
            if (stopDepartureTimes.get(insertionPoint) < departureTime) {
                throw new TripOvertakingException(trips.get(insertionPoint + 1), trip, stopIndex);
            }
        }
        if (stopIndex == 0) {
            trips.add(insertionPoint, trip);
            perTripFlags.add(insertionPoint, 0);
        }
        stopDepartureTimes.add(insertionPoint, departureTime);
        stopRunningTimes.add(insertionPoint, runningTime);
        stopArrivalTimes.add(insertionPoint, arrivalTime);
        stopDwellTimes.add(insertionPoint, dwellTime);
    }

    public int getNextTrip(int stopIndex, int afterTime, boolean wheelchairAccessible,
            boolean bikesAllowed, boolean pickup) {
        int flag = pickup ? FLAG_PICKUP : FLAG_DROPOFF;
        if ((perStopFlags[stopIndex] & flag) == 0) {
            return -1;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }
        ArrayList<Integer> stopDepartureTimes = departureTimes[stopIndex];
        int index = Collections.binarySearch(stopDepartureTimes, afterTime);
        if (index == -stopDepartureTimes.size() - 1)
            return -1;

        if (index < 0) {
            index = -index - 1;
        }

        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            while ((perTripFlags.get(index) & flags) == 0) {
                index ++;
                if (index == perTripFlags.size()) {
                    return -1;
                }
            }
        }
        return index;
    }

    public int getRunningTime(int stopIndex, int trip) {
        ArrayList<Integer> stopRunningTimes = runningTimes[stopIndex];
        return stopRunningTimes.get(trip);
    }

    public int getDepartureTime(int stopIndex, int trip) {
        ArrayList<Integer> stopDepartureTimes = departureTimes[stopIndex];
        return stopDepartureTimes.get(trip);
    }

    public int getDepartureTimeInsertionPoint(int departureTime) {
        ArrayList<Integer> stopDepartureTimes = departureTimes[0];
        int index = Collections.binarySearch(stopDepartureTimes, departureTime);
        return -index - 1;
    }

    public int getPreviousTrip(int stopIndex, int beforeTime, boolean wheelchairAccessible,
            boolean bikesAllowed, boolean pickup) {
        int flag = pickup ? FLAG_PICKUP : FLAG_DROPOFF;
        if ((perStopFlags[stopIndex + 1] & flag) == 0) {
            return -1;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex + 1] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }
        ArrayList<Integer>[] arrivals = arrivalTimes;
        if (arrivals == null) {
            arrivals = departureTimes;
            stopIndex += 1;
        }
        ArrayList<Integer> stopArrivalTimes = arrivals[stopIndex];
        int index = Collections.binarySearch(stopArrivalTimes, beforeTime);
        if (index == -1)
            return -1;

        if (index < 0) {
            index = -index - 2;
        }

        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            while ((perTripFlags.get(index) & flags) == 0) {
                index --;
                if (index == -1) {
                    return -1;
                }
            }
        }
        return index;
    }

    public int getArrivalTime(int stopIndex, int trip) {
        ArrayList<Integer>[] arrivals = arrivalTimes;
        if (arrivals == null) {
            arrivals = departureTimes;
            stopIndex += 1;
        }
        ArrayList<Integer> stopArrivalTimes = arrivals[stopIndex];
        return stopArrivalTimes.get(trip);
    }

    public int getDwellTime(int stopIndex, int trip) {
        ArrayList<Integer> stopDwellTimes = dwellTimes[stopIndex];
        return stopDwellTimes.get(trip);
    }

    public void setDwellTime(int stopIndex, int trip, int dwellTime) {
        ArrayList<Integer> stopDwellTimes = dwellTimes[stopIndex];
        stopDwellTimes.set(trip, dwellTime);
    }

    public Iterator<Integer> getDepartureTimes(int stopIndex) {
        return departureTimes[stopIndex].iterator();
    }

    public boolean getWheelchairAccessible(int stopIndex, int trip) {
        if ((perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return false;
        }
        if ((perTripFlags.get(trip) & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return false;
        }
        return true;
    }

    public boolean getBikesAllowed(int trip) {
        return (perTripFlags.get(trip) & FLAG_BIKES_ALLOWED) != 0;
    }

    public Trip getTrip(int trip) {
        return trips.get(trip);
    }
    
    public List<Trip> getTrips() {
    	return trips;
    }

    public int getNumDwells() {
        return dwellTimes[0].size();
    }

    /*
     * Attempt to simplify this pattern by removing dwell times and using arrival times for
     * departure times. For transit agencies that do not distinguish arrival and departure times,
     * such as New York, this will save memory.
     */
    public void simplify() {

        for (int stopId = 0; stopId < departureTimes.length; ++stopId) {
            ArrayList<Integer> stopDwells = dwellTimes[stopId];
            for (int trip = 0; trip < stopDwells.size(); ++trip) {
                if (stopDwells.get(trip) > 0) {
                    return;
                }
            }
        }

        dwellTimes = null;
        departureTimes = Arrays.copyOf(departureTimes, departureTimes.length + 1);
        departureTimes[departureTimes.length - 1] = arrivalTimes[arrivalTimes.length - 1];
        arrivalTimes = null;
    }

    public boolean canAlight(int stopIndex) {
        return (perStopFlags[stopIndex] & FLAG_DROPOFF) != 0;
    }

    public boolean canBoard(int stopIndex) {
        return (perStopFlags[stopIndex] & FLAG_PICKUP) != 0;
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        perTripFlags.trimToSize();
        out.defaultWriteObject();
    }

    public String getZone(int stopIndex) {
        return zones[stopIndex];
    }
    
    /**
     * Replace arraylists with arrays to save memory.
     * @return
     */
    public ArrayTripPattern convertToArrayTripPattern() {
        if (arrayPattern == null) {
            arrayPattern = new ArrayTripPattern(exemplar, departureTimes, runningTimes, arrivalTimes, dwellTimes, zones, perTripFlags, perStopFlags, trips);
            departureTimes = runningTimes = arrivalTimes = dwellTimes = null;
            zones = null;
            perTripFlags = null;
            perStopFlags = null;
            trips = null;
        }
        return arrayPattern;
    }

    @Override
    public Trip getExemplar() {
        return exemplar;
    }
    
    /*
     * This is a slow implementation because this class should not be used in production. 
     */
    @Override
    public int getBestRunningTime(int stopIndex) {
        int bestRunningTime = Integer.MAX_VALUE;
        for (int runningTime : runningTimes[stopIndex]) {
            if (bestRunningTime > runningTime) {
                bestRunningTime = runningTime;
            }
        }
        return bestRunningTime;
    }
    
    /*
     * This is a slow implementation because this class should not be used in production. 
     */
    @Override
    public int getBestDwellTime(int stopIndex) {
        if (dwellTimes == null) {
            return 0;
        }
        int bestDwellTime = Integer.MAX_VALUE;
        for (int dwellTime : dwellTimes[stopIndex]) {
            if (bestDwellTime > dwellTime) {
                bestDwellTime = dwellTime;
            }
        }
        return bestDwellTime;        
    }

    public int getPatternIndex(Trip trip) {
        return trips.indexOf(trip);
    }
    
    public boolean stopTimesIdentical (List<StopTime> stopTimes, int insertionPoint) {
        if (arrivalTimes.length != stopTimes.size() - 1) {
            return false;
        }
        for (int i = 0; i<arrivalTimes.length; i++) {
            if (stopTimes.get(i).getDepartureTime() != departureTimes[i].get(insertionPoint))
                return false;
            if (stopTimes.get(i+1).getArrivalTime() != arrivalTimes[i].get(insertionPoint))
                return false;
        }
        return true;
    }
}
