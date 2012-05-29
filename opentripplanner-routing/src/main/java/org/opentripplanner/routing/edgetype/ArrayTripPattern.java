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
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.core.GraphBuilderAnnotation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.GraphBuilderAnnotation.Variety;
import org.opentripplanner.routing.edgetype.factory.TripOvertakingException;

/**
 * A memory-efficient implementation of TripPattern
 */
public class ArrayTripPattern implements TripPattern, Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    public final Trip exemplar;

    private String[][] headsigns;

    private final ArrayList<TripTimes> tripTimes = new ArrayList<TripTimes>();

    @XmlElement
    private String[] zones;

    @XmlElement
    private final ArrayList<Integer> perTripFlags = new ArrayList<Integer>();

    @XmlElement
    private int[] perStopFlags;

    private final ArrayList<Trip> trips = new ArrayList<Trip>();

    int bestRunningTimes[];

    int bestDwellTimes[];

    public ArrayTripPattern(Trip exemplar) { //ScheduledStopPattern stopPattern) {
        this.exemplar = exemplar;
    }

    public void setStopFlags(int trip, int flags) {
        perStopFlags[trip] = flags;
    }

    // finish off the pattern once all times have been added 
    // maybe trim arrays too
    public void finalize() {
        this.zones = zones;
        this.perStopFlags = perStopFlags;
        this.bestRunningTimes = new int[nHops];
        this.bestDwellTimes = nullArrivals ? null : new int[nHops];
        if ( ! nullArrivals) {
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
        
    }
    
    @Override
    public TripTimes getNextTrip(int stopIndex, int afterTime, RoutingRequest options) {
        boolean pickup = true;
        boolean wheelchairAccessible = options.wheelchairAccessible;
        boolean bikesAllowed = options.modes.getBicycle();
        
        int mask = pickup ? MASK_PICKUP : MASK_DROPOFF;
        int shift = pickup ? SHIFT_PICKUP : SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex] & mask) >> shift == NO_PICKUP) {
            return null;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return null;
        }
        TripTimes bestTrip = null;
        int bestTime = Integer.MAX_VALUE;
        int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
        // linear search... because trips may change with stoptime updates, we cannot count on them being sorted
        for (int i = 0; i < trips.size(); i++) {
            // grab a reference in case it is swapped out by an update
            TripTimes currTrip = tripTimes.get(i); 
            int currTime = currTrip.getDepartureTime(stopIndex);
            boolean acceptableTrip = flags == 0 ? true : (perTripFlags.get(i) & flags) != 0; 
            if (currTime > afterTime && currTime < bestTime && acceptableTrip && 
                ! options.bannedTrips.contains(trips.get(i).getId())) {
                bestTrip = currTrip;
                bestTime = currTime;
            }
        }
        return bestTrip;
    }
    
    @Override
    public TripTimes getPreviousTrip(int stopIndex, int beforeTime, RoutingRequest options) {
        boolean pickup = false;
        boolean wheelchairAccessible = options.wheelchairAccessible;
        boolean bikesAllowed = options.modes.getBicycle();

        int mask = pickup ? MASK_PICKUP : MASK_DROPOFF;
        int shift = pickup ? SHIFT_PICKUP : SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex + 1] & mask) >> shift == NO_PICKUP) {
            return null;
        }
        if (wheelchairAccessible && (perStopFlags[stopIndex + 1] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return null;
        }
        TripTimes bestTrip = null;
        int bestTime = Integer.MIN_VALUE;
        int flags = (bikesAllowed ? FLAG_BIKES_ALLOWED : 0) | (wheelchairAccessible ? FLAG_WHEELCHAIR_ACCESSIBLE : 0);
        // linear search... because trips may change with stoptime updates, we cannot count on them being sorted
        for (int i = 0; i < trips.size(); i++) {
            // grab reference in case it is swapped out by an update
            TripTimes currTrip = tripTimes.get(i); 
            int currTime = currTrip.getDepartureTime(stopIndex);
            boolean acceptableTrip = flags == 0 ? true : (perTripFlags.get(i) & flags) != 0; 
            if (currTime < beforeTime && currTime > bestTime && acceptableTrip &&
                ! options.bannedTrips.contains(trips.get(i).getId())) {
                bestTrip = currTrip;
                bestTime = currTime;
            }
        }
        return bestTrip;
    }

    public int getDepartureTime(int hop, int trip) {
        return tripTimes.get(trip).getDepartureTime(hop);
    }

    public int getArrivalTime(int hop, int trip) {
        return tripTimes.get(trip).getArrivalTime(hop);
    }

    public int getRunningTime(int stopIndex, int trip) {
        return tripTimes.get(trip).getRunningTime(stopIndex);
    }

    public int getDwellTime(int hop, int trip) {
        // the dwell time of a hop is the dwell time *before* that hop.
        return tripTimes.get(trip).getDwellTime(hop);
    }

    public Iterator<Integer> getDepartureTimes(int stopIndex) {
        return new DeparturesIterator(stopIndex);
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

    public Trip getTrip(int tripIndex) {
        return trips.get(tripIndex);
    }
    
    @XmlTransient
    public List<Trip> getTrips() {
    	return trips;
    }

    public int getTripIndex(Trip trip) {
        return trips.indexOf(trip);
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
    
    public class DeparturesIterator implements Iterator<Integer> {

        int nextPosition = 0;

        private int stopIndex;

        public DeparturesIterator(int stopIndex) {
            this.stopIndex = stopIndex;
        }

        @Override
        public boolean hasNext() {
            return nextPosition < trips.size();
        }

        @Override
        public Integer next() {
            return tripTimes.get(nextPosition++).departureTimes[stopIndex];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    @Override
    public TripTimes getTripTimes(int tripIndex) {
        return tripTimes.get(tripIndex);
    }

    public void addTrip(Trip trip, List<StopTime> stopTimes) {
        // TODO: double-check that the stops and pickup/dropoffs are right for this trip
        int index = tripTimes.size();
        tripTimes.add(new TripTimes(trip, index, stopTimes));
        trips.add(trip);
        // this could be replaced with reading directly from the Trip object in TripTimes
        perTripFlags.add(
                (trip.getWheelchairAccessible() == 1 ? TripPattern.FLAG_WHEELCHAIR_ACCESSIBLE : 0)
                | 
                (((trip.getRoute().getBikesAllowed() == 2 && trip.getTripBikesAllowed() != 1) 
                || trip.getTripBikesAllowed() == 2) ? TripPattern.FLAG_BIKES_ALLOWED : 0));
    }
}
