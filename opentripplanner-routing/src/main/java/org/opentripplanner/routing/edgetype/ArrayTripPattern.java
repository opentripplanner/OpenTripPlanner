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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;

/**
 * A memory-efficient implementation of TripPattern
 */
public class ArrayTripPattern implements TripPattern, Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private Trip exemplar;

    private String[][] headsigns;

    private TripTimes[] tripTimes;

    @XmlElement
    private String[] zones;

    @XmlElement
    private int[] perTripFlags;

    @XmlElement
    private int[] perStopFlags;

    private Trip[] trips;

    int bestRunningTimes[];

    int bestDwellTimes[];

    public ArrayTripPattern(Trip exemplar, ArrayList<Integer>[] departureTimes,
            ArrayList<Integer>[] runningTimes, ArrayList<Integer>[] arrivalTimes,
            ArrayList<Integer>[] dwellTimes, ArrayList<String>[] headsigns, String[] zones, ArrayList<Integer> perTripFlags,
            int[] perStopFlags, ArrayList<Trip> trips) {
        
        int nHops  = runningTimes.length;
        int nStops = nHops + 1;
        int nTrips = departureTimes[0].size(); 
        // maybe check against other incoming array sizes, or trips
        
        this.exemplar = exemplar;
        
        // transposing from stop-trip TO TRIP-STOP
        this.tripTimes = new TripTimes[nTrips];
        boolean nullArrivals = arrivalTimes == null;
        for (int i = 0; i < tripTimes.length; i++)
            tripTimes[i] = new TripTimes(nStops, nullArrivals);

        this.zones = zones;
        this.perTripFlags = new int[nTrips];
        this.perStopFlags = perStopFlags;
        this.bestRunningTimes = new int[nHops];
        this.bestDwellTimes = nullArrivals ? null : new int[nHops];
        this.trips = new Trip[nTrips];

        for (int h = 0; h < departureTimes.length; ++h) {
            for (int t = 0; t < nTrips; ++t) {
                this.tripTimes[t].departureTimes[h] = departureTimes[h].get(t); // transpose
            }
        }
        if ( ! nullArrivals) {
            for (int h = 0; h < nHops; ++h) {
                for (int t = 0; t < nTrips; ++t) {
                    this.tripTimes[t].arrivalTimes[h] = arrivalTimes[h].get(t); // transpose
                }
            }
            for (int h = 1; h < nHops; ++h) { // dwell time is undefined on first hop
                bestDwellTimes[h] = Integer.MAX_VALUE;
                for (int t = 0; t < nTrips; ++t) {
                    int dt = this.getDwellTime(h,  t);
                    if (bestDwellTimes[h] > dt) {
                        bestDwellTimes[h] = dt;
                    }
                }
            }
        }
        // FIXME: why is incoming running times 1 shorter than departures?
        // because when there are no arrivals array, the last departure is actually used for an arrival 
        for (int h = 0; h < nHops; ++h) {
            bestRunningTimes[h] = Integer.MAX_VALUE;
            for (int t = 0; t < nTrips; ++t) { // iterate over trip times
                int rt = this.getRunningTime(h, t);
                if (bestRunningTimes[h] > rt) {
                    bestRunningTimes[h] = rt;
                }
            }
        }

        if (headsigns != null) { 
            // DO NOT transpose headsigns to allow repeating empty rows
            this.headsigns = new String[nHops][nTrips]; 
            String[] nullRow = null; 
            // headsigns contains 1 less headsign than there are stops, because sign change is pointless at the last stop
            for (int s = 0; s < nHops; ++s) { 
        		boolean rowIsNull = true;
        		for (int t = 0; t < headsigns[s].size(); ++t) {
        			this.headsigns[s][t] = headsigns[s].get(t); 
        			if (this.headsigns[s][t] != null) {
        				rowIsNull = false; 
        			}
        		}
        		if (rowIsNull) {
        		    // repeat the same row object when empty row encountered
        			if (nullRow == null) {
        				nullRow = this.headsigns[s];
        			} else {
        				this.headsigns[s] = nullRow;
        			}
        		}
            }
        }
        for (int t = 0; t < nTrips; ++t) {
            this.perTripFlags[t] = perTripFlags.get(t);
        }
        for (int t = 0; t < nTrips; ++t) {
            this.trips[t] = trips.get(t);
        }
    }

    // based on Arrays.binarySearch source, but always returns a positive int
    private int findDepartureAfter(int stopIndex, int key) {
        int low = 0;
        int high = trips.length - 1;
        int mid = 0;
        while (low <= high) {
            mid = (low + high) >>> 1;
            final int d = tripTimes[mid].departureTimes[stopIndex];
            if (d == key)
                return mid;
            else if (d > key) 
                high = mid - 1;
            else
                low = ++mid;
        }
        return mid;
    }
    
    // based on Arrays.binarySearch source, but always returns a positive int
    private int findArrivalBefore(int stopIndex, int key) {
        int low = 0;
        int high = trips.length - 1;
        int mid = 0;
        while (low <= high) {
            mid = (low + high) >>> 1;
            final int d; 
            if (tripTimes[mid].arrivalTimes == null)
                d = tripTimes[mid].departureTimes[stopIndex + 1]; // handle with instance methods on TripTimes?
            else
                d = tripTimes[mid].arrivalTimes[stopIndex];
            if (d == key) // reverse optimizing depends on finding trips that leave exactly at the given time
                return mid;

            else if (d > key) 
                high = --mid; // is this correct ? 
            else
                low = mid + 1;
        }
        return mid; // 
    }

    public int getNextTrip(int stopIndex, int afterTime, boolean wheelchairAccessible,
            boolean bikesAllowed, boolean pickup) {
        int mask = pickup ? MASK_PICKUP : MASK_DROPOFF;
        int shift = pickup ? SHIFT_PICKUP : SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex] & mask) >> shift == NO_PICKUP) {
            return -1;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }
        int tripIndex = findDepartureAfter(stopIndex, afterTime);
        if (tripIndex == trips.length) {
            return -1;
        }
        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            while ((perTripFlags[tripIndex] & flags) == 0) {
                tripIndex++;
                if (tripIndex == perTripFlags.length) {
                    return -1;
                }
            }
        }
        return tripIndex;
    }

    public int getPreviousTrip(int stopIndex, int beforeTime, boolean wheelchairAccessible,
            boolean bikesAllowed, boolean pickup) {
        int mask = pickup ? MASK_PICKUP : MASK_DROPOFF;
        int shift = pickup ? SHIFT_PICKUP : SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex + 1] & mask) >> shift == NO_PICKUP) {
            return -1;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex + 1] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }
        int tripIndex = findArrivalBefore(stopIndex, beforeTime);
        if (tripIndex == -1) { // passed 0, no trip before the given time
            return -1;
        }
        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            while ((perTripFlags[tripIndex] & flags) == 0) {
                tripIndex--;
                if (tripIndex == -1) {
                    return -1;
                }
            }
        }
        return tripIndex;
    }

    public int getDepartureTime(int hop, int trip) {
        return tripTimes[trip].getDepartureTime(hop);
    }

    public int getArrivalTime(int hop, int trip) {
        return tripTimes[trip].getArrivalTime(hop);
    }

    public int getRunningTime(int stopIndex, int trip) {
        return getArrivalTime(stopIndex, trip) - getDepartureTime(stopIndex, trip);
    }

    public int getDwellTime(int hop, int trip) {
        // the dwell time of a hop is the dwell time *before* that hop.
        // Therefore it is undefined for hop 0, and at the end of a trip.
        // see GTFSPatternHopFactory.makeTripPattern()
        int arrivalTime = getArrivalTime(hop-1, trip);
        int departureTime = getDepartureTime(hop, trip);
        return departureTime - arrivalTime;
    }

    public Iterator<Integer> getDepartureTimes(int stopIndex) {
        return new DeparturesIterator(stopIndex);
    }

    public boolean getWheelchairAccessible(int stopIndex, int trip) {
        if ((perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return false;
        }
        if ((perTripFlags[trip] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return false;
        }
        return true;
    }

    public boolean getBikesAllowed(int trip) {
        return (perTripFlags[trip] & FLAG_BIKES_ALLOWED) != 0;
    }

    public Trip getTrip(int tripIndex) {
        return trips[tripIndex];
    }
    
    @XmlTransient
    public List<Trip> getTrips() {
    	return Arrays.asList(trips);
    }

    public boolean canAlight(int stopIndex) {
        return getAlightType(stopIndex) != NO_PICKUP;
    }

    public boolean canBoard(int stopIndex) {
        return getBoardType(stopIndex) != NO_PICKUP;
    }

    public String getZone(int stopIndex) {
        return zones[stopIndex];
    }

    @Override
    public Trip getExemplar() {
        return exemplar;
    }

    @Override
    public int getBestRunningTime(int stopIndex) {
        return bestRunningTimes[stopIndex];
    }

    @Override
    public int getBestDwellTime(int stopIndex) {
        if (bestDwellTimes == null) {
            return 0;
        }
        return bestDwellTimes[stopIndex];
    }

    @Override
    public String getHeadsign(int stopIndex, int trip) {
        if (headsigns == null) {
            return null;
        }
        return headsigns[stopIndex][trip];
    }

    @Override
    public int getAlightType(int stopIndex) {
        return (perStopFlags[stopIndex] & MASK_DROPOFF) >> SHIFT_DROPOFF;
    }

    @Override
    public int getBoardType(int stopIndex) {
        return (perStopFlags[stopIndex] & MASK_PICKUP) >> SHIFT_PICKUP;
    }
    
    /**
     * Consider the following code: 
     * int myArray[] = new int[5]; 
     * something = Arrays.asList(myArray); 
     * You  would think that something would have type List<Integer>, but in fact 
     * it has type List<int[]>. This is because Java autoboxing is completely broken. 
     * So, this class.
     */
    public class DeparturesIterator implements Iterator<Integer> {

        int nextPosition = 0;

        private int stopIndex;

        public DeparturesIterator(int stopIndex) {
            this.stopIndex = stopIndex;
        }

        @Override
        public boolean hasNext() {
            return nextPosition < trips.length;
        }

        @Override
        public Integer next() {
            return tripTimes[nextPosition++].departureTimes[stopIndex];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
