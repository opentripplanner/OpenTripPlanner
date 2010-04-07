package org.opentripplanner.routing.edgetype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.FareContext;

/**
 * Consider the following code:
 * int myArray[] = new int[5];
 * something = Arrays.asList(myArray);
 * You would think that something would have type List<Integer>, but in fact it has type List<int[]>.  
 * This is because Java autoboxing is royally fucked.  So, this class.
 */
class IntArrayIterator implements Iterator<Integer> {

    int nextPosition = 0;
    private int[] array;
    public IntArrayIterator(int[] array) {
        this.array = array;
    }
    
    @Override
    public boolean hasNext() {
        return nextPosition == array.length;
    }

    @Override
    public Integer next() {
        return array[nextPosition++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
}

/**
 * A memory-efficient implementation of TripPattern
 */
public class ArrayTripPattern implements TripPattern, Serializable {

    private static final long serialVersionUID = -1283975534796913802L;
    
    private Trip exemplar;
    private int[][] departureTimes;
    private int[][] runningTimes;
    private int[][] arrivalTimes;
    private int[][] dwellTimes;
    private String[] zones;
    private int[] perTripFlags;
    private int[] perStopFlags;
    private Trip[] trips;
    private FareContext fareContext;

    public ArrayTripPattern(Trip exemplar, ArrayList<Integer>[] departureTimes,
            ArrayList<Integer>[] runningTimes, ArrayList<Integer>[] arrivalTimes,
            ArrayList<Integer>[] dwellTimes, String[] zones, ArrayList<Integer> perTripFlags,
            int[] perStopFlags, ArrayList<Trip> trips, FareContext fareContext) {
        this.exemplar = exemplar;
        this.departureTimes = new int[departureTimes.length][departureTimes[0].size()];
        this.runningTimes = new int[runningTimes.length][runningTimes[0].size()];
        if (arrivalTimes == null) {
            this.arrivalTimes = null;
            this.dwellTimes = null;
        } else {
            this.arrivalTimes = new int[arrivalTimes.length][arrivalTimes[0].size()];
            this.dwellTimes = new int[dwellTimes.length][dwellTimes[0].size()];
        }
        this.zones = zones;
        this.perTripFlags = new int[perTripFlags.size()];
        this.perStopFlags = perStopFlags;
        this.trips = new Trip[trips.size()];
        this.fareContext = fareContext;
        
        for (int i = 0; i < departureTimes.length; ++i) {
            for (int j = 0; j < departureTimes[i].size(); ++j) {
                this.departureTimes[i][j] = departureTimes[i].get(j);
            }
        }
        for (int i = 0; i < runningTimes.length; ++i) {
            for (int j = 0; j < runningTimes[i].size(); ++j) {
                this.runningTimes[i][j] = runningTimes[i].get(j);
            }
        }
        if (this.arrivalTimes != null) {
            for (int i = 0; i < arrivalTimes.length; ++i) {
                for (int j = 0; j < arrivalTimes[i].size(); ++j) {
                    this.arrivalTimes[i][j] = arrivalTimes[i].get(j);
                }
            }
            for (int i = 0; i < dwellTimes.length; ++i) {
                for (int j = 0; j < dwellTimes[i].size(); ++j) {
                    this.dwellTimes[i][j] = dwellTimes[i].get(j);
                }
            }
        }
        for (int i = 0; i < perTripFlags.size(); ++i) {
            this.perTripFlags[i] = perTripFlags.get(i);
        }
        for (int i = 0; i < trips.size(); ++i) {
            this.trips[i] = trips.get(i);
        }
    }

    public int getNextTrip(int stopIndex, int afterTime, boolean wheelchairAccessible, boolean pickup) {
        int flag = pickup ? FLAG_PICKUP : FLAG_DROPOFF;
        if ((perStopFlags[stopIndex] & flag) == 0) {
            return -1;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }
        int[] stopDepartureTimes = departureTimes[stopIndex];
        int index = Arrays.binarySearch(stopDepartureTimes, afterTime);
        if (index == -stopDepartureTimes.length - 1)
            return -1;

        if (index < 0) {
            index = -index - 1;
        }

        if (wheelchairAccessible) {
            while ((perTripFlags[index] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
                index ++;
                if (index == perTripFlags.length) {
                    return -1;
                }
            }
        }
        return index;
    }

    public int getRunningTime(int stopIndex, int trip) {
        int[] stopRunningTimes = runningTimes[stopIndex];
        return stopRunningTimes[trip];
    }

    public int getDepartureTime(int stopIndex, int trip) {
        int[] stopDepartureTimes = departureTimes[stopIndex];
        return stopDepartureTimes[trip];
    }

    public int getPreviousTrip(int stopIndex, int beforeTime, boolean wheelchairAccessible, boolean pickup) {
        int flag = pickup ? FLAG_PICKUP : FLAG_DROPOFF;
        if ((perStopFlags[stopIndex + 1] & flag) == 0) {
            return -1;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex + 1] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return -1;
        }
        int[][] arrivals = arrivalTimes;
        if (arrivals == null) {
            arrivals = departureTimes;
            stopIndex += 1;
        }
        int[] stopArrivalTimes = arrivals[stopIndex];
        int index = Arrays.binarySearch(stopArrivalTimes, beforeTime);
        if (index == -1)
            return -1;

        if (index < 0) {
            index = -index - 2;
        }

        if (wheelchairAccessible) {
            while ((perTripFlags[index] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
                index --;
                if (index == -1) {
                    return -1;
                }
            }
        }
        return index;
    }

    public int getArrivalTime(int stopIndex, int trip) {
        int[][] arrivals = arrivalTimes;
        if (arrivals == null) {
            arrivals = departureTimes;
            stopIndex += 1;
        }
        int[] stopArrivalTimes = arrivals[stopIndex];
        return stopArrivalTimes[trip];
    }

    public int getDwellTime(int stopIndex, int trip) {
        int[] stopDwellTimes = dwellTimes[stopIndex];
        return stopDwellTimes[trip];
    }

    public Iterator<Integer> getDepartureTimes(int stopIndex) {
        return new IntArrayIterator(departureTimes[stopIndex]);
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

    public Trip getTrip(int tripIndex) {
        return trips[tripIndex];
    }

    public boolean canAlight(int stopIndex) {
        return (perStopFlags[stopIndex] & FLAG_DROPOFF) != 0;
    }

    public boolean canBoard(int stopIndex) {
        return (perStopFlags[stopIndex] & FLAG_PICKUP) != 0;
    }

    public String getZone(int stopIndex) {
        return zones[stopIndex];
    }
    
    public FareContext getFareContext() {
        return fareContext;
    }

    @Override
    public Trip getExemplar() {
        return exemplar;
    }
}
