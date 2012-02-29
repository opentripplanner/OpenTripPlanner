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

/**
 * Consider the following code: 
 * int myArray[] = new int[5]; 
 * something = Arrays.asList(myArray); 
 * You  would think that something would have type List<Integer>, but in fact 
 * it has type List<int[]>. This is because Java autoboxing is completely broken. 
 * So, this class.
 */
class IntArrayIterator implements Iterator<Integer> {

    int nextPosition = 0;

    private int[] array;

    public IntArrayIterator(int[] array) {
        this.array = array;
    }

    @Override
    public boolean hasNext() {
        return nextPosition < array.length;
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

    /*
     * All of these 2d arrays are [stop][trip].
     */
    private Trip exemplar;

    @XmlElement
    private int[][] departureTimes;

    private int[][] runningTimes;

    @XmlElement
    private int[][] arrivalTimes;

    private int[][] dwellTimes;

    private String[][] headsigns;

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
        this.exemplar = exemplar;
        this.departureTimes = new int[departureTimes.length][departureTimes[0].size()];
        this.runningTimes = new int[runningTimes.length][runningTimes[0].size()];
        if (headsigns != null) { 
        	this.headsigns = new String[headsigns.length][headsigns[0].size()];
        }
        if (arrivalTimes == null) {
            this.arrivalTimes = null;
            this.dwellTimes = null;
            this.bestDwellTimes = null;
        } else {
            this.arrivalTimes = new int[arrivalTimes.length][arrivalTimes[0].size()];
            this.dwellTimes = new int[dwellTimes.length][dwellTimes[0].size()];
            this.bestDwellTimes = new int[perStopFlags.length];
        }
        this.zones = zones;
        this.perTripFlags = new int[perTripFlags.size()];
        this.perStopFlags = perStopFlags;
        this.bestRunningTimes = new int[perStopFlags.length];
        this.trips = new Trip[trips.size()];

        for (int i = 0; i < departureTimes.length; ++i) {
            for (int j = 0; j < departureTimes[i].size(); ++j) {
                this.departureTimes[i][j] = departureTimes[i].get(j);
            }
        }
        for (int i = 0; i < runningTimes.length; ++i) {
            bestRunningTimes[i] = Integer.MAX_VALUE;
            for (int j = 0; j < runningTimes[i].size(); ++j) {
                this.runningTimes[i][j] = runningTimes[i].get(j);
                if (bestRunningTimes[i] > this.runningTimes[i][j]) {
                    bestRunningTimes[i] = this.runningTimes[i][j];
                }
            }
        }
        if (this.arrivalTimes != null) {
            for (int i = 0; i < arrivalTimes.length; ++i) {
                for (int j = 0; j < arrivalTimes[i].size(); ++j) {
                    this.arrivalTimes[i][j] = arrivalTimes[i].get(j);
                }
            }
            for (int i = 0; i < dwellTimes.length; ++i) {
                bestDwellTimes[i] = Integer.MAX_VALUE;
                for (int j = 0; j < dwellTimes[i].size(); ++j) {
                    this.dwellTimes[i][j] = dwellTimes[i].get(j);
                    if (bestDwellTimes[i] > this.dwellTimes[i][j]) {
                        bestDwellTimes[i] = this.dwellTimes[i][j];
                    }
                }
            }
        }
        if (headsigns != null) { 
        	String[] nullRow = null;
        	
        	for (int i = 0; i < headsigns.length; ++i) {
        		boolean rowIsNull = true;
        		for (int j = 0; j < headsigns[i].size(); ++j) {
        			this.headsigns[i][j] = headsigns[i].get(j);
        			if (this.headsigns[i][j] != null) {
        				rowIsNull = false; 
        			}
        		}
        		if (rowIsNull) {
        			if (nullRow == null) {
        				nullRow = this.headsigns[i];
        			} else {
        				this.headsigns[i] = nullRow;
        			}
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
        int[] stopDepartureTimes = departureTimes[stopIndex];
        int index = Arrays.binarySearch(stopDepartureTimes, afterTime);
        if (index == -stopDepartureTimes.length - 1)
            return -1;

        if (index < 0) {
            index = -index - 1;
        }

        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            while ((perTripFlags[index] & flags) == 0) {
                index++;
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

        if (wheelchairAccessible || bikesAllowed) {
            int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
            while ((perTripFlags[index] & flags) == 0) {
                index--;
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
}
