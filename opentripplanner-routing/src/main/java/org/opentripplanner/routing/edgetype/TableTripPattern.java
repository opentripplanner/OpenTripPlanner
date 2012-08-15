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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.trippattern.UpdateList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a class of trips distinguished by service id and list of stops. For each stop, there
 * is a list of departure times, running times, arrival times, dwell times, and wheelchair
 * accessibility information (one of each of these per trip per stop). An exemplar trip is also
 * included so that information such as route name can be found. Trips are assumed to be
 * non-overtaking, so that an earlier trip never arrives after a later trip.
 */
public class TableTripPattern implements TripPattern, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TableTripPattern.class);

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    public static final int FLAG_WHEELCHAIR_ACCESSIBLE = 1;
    public static final int MASK_PICKUP = 2|4;
    public static final int SHIFT_PICKUP = 1;
    public static final int MASK_DROPOFF = 8|16;
    public static final int SHIFT_DROPOFF = 3;
    public static final int NO_PICKUP = 1;
    public static final int FLAG_BIKES_ALLOWED = 32;
    
    /** an integer index uniquely identifying this pattern among all in the graph.
     *  this additional level of indirection allows versioning of trip patterns, which is 
     *  necessary for real-time stop time updates. */
//    public final int patternIndex;
    
    /* an arbitrary trip that uses this pattern */
    public final Trip exemplar;

    // override trip_headsign with stop_headsign where necessary
    private final List<List<String>> headsigns = new ArrayList<List<String>>();

    private final ArrayList<TripTimes> tripTimes = new ArrayList<TripTimes>();

//    @XmlElement
//    private final ArrayList<Integer> perTripFlags = new ArrayList<Integer>();

    // redundant since tripTimes have a trip
    private final ArrayList<Trip> trips = new ArrayList<Trip>();

    // all trips in a pattern have the same stops, so this applies to every trip in this pattern
    public Stop[] stops; 

    @XmlElement
    private int[] perStopFlags;
    
    //@XmlElement
    //private String[] zones; // use stops instead

    int bestRunningTimes[];
    
    int bestDwellTimes[];

    int serviceId; // optimized serviceId code
    
    public TableTripPattern(Trip exemplar, ScheduledStopPattern stopPattern, int serviceId) {
        this.exemplar = exemplar;
        this.serviceId = serviceId;
        setStopsFromStopPattern(stopPattern);
    }

    private void setStopsFromStopPattern(ScheduledStopPattern stopPattern) {
        this.stops = new Stop[stopPattern.stops.size()];
        perStopFlags = new int[stops.length];
        int i = 0;
        for (Stop stop : stopPattern.stops) {
            this.stops[i] = stop;
            if (stop.getWheelchairBoarding() == 1) {
                perStopFlags[i] |= FLAG_WHEELCHAIR_ACCESSIBLE;
            }
            perStopFlags[i] |= stopPattern.pickups.get(i) << SHIFT_PICKUP;
            perStopFlags[i] |= stopPattern.dropoffs.get(i) << SHIFT_DROPOFF;
            ++i;
        }
    }

    // finish off the pattern once all times have been added 
    // cache best (lowest) running times and dwell times; maybe trim arrays too
    public void finish() {
        int nHops = stops.length - 1;
        int nTrips = trips.size();
        this.bestRunningTimes = new int[nHops];
        boolean nullArrivals = false; // TODO: should scan through triptimes?
        if ( ! nullArrivals) {
            this.bestDwellTimes = new int[nHops];
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
            for (int t = 0; t < nTrips; ++t) { 
                int rt = this.getRunningTime(h, t);
                if (bestRunningTimes[h] > rt) {
                    bestRunningTimes[h] = rt;
                }
            }
        }
    }
    
    private Boolean tripAcceptable(Trip trip, boolean bicycle, boolean wheelchair) {
        boolean result = true;
        if (wheelchair)
            result &= trip.getWheelchairAccessible() == 1;
        if (bicycle)
            result &= trip.getTripBikesAllowed() == 2 ||
            (trip.getRoute().getBikesAllowed() == 2 && trip.getTripBikesAllowed() != 1); 
        return result;
    }
    
    /** 
     * Get the index of the next trip that has a stop after (or at) 
     * afterTime at the stop at stopIndex. 
     * The haveBicycle parameter must be passed in because we cannot determine whether the user
     * is in possession of a rented bicycle from the options alone.
     */
    public TripTimes getNextTrip(int stopIndex, int afterTime, boolean haveBicycle,
            RoutingRequest options) {
        boolean pickup = true;
        int mask = pickup ? MASK_PICKUP : MASK_DROPOFF;
        int shift = pickup ? SHIFT_PICKUP : SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex] & mask) >> shift == NO_PICKUP) {
            return null;
        }
        boolean wheelchair = options.wheelchairAccessible;
        if (wheelchair && (perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return null;
        }
        // linear search:
        // because trips may change with stoptime updates, we cannot count on them being sorted
        TripTimes bestTrip = null;
        int bestTime = Integer.MAX_VALUE;
        for (int i = 0; i < trips.size(); i++) {
            // grab a reference before tests in case it is swapped out by an update thread
            TripTimes currTrip = tripTimes.get(i); 
            int currTime = currTrip.getDepartureTime(stopIndex);
            if (currTime >= afterTime && currTime < bestTime && 
                    tripAcceptable(currTrip.trip, haveBicycle, wheelchair) && 
                    ! options.bannedTrips.contains(trips.get(i).getId())) {
                bestTrip = currTrip;
                bestTime = currTime;
            }
        }
        return bestTrip;
    }
    
    /** 
     * Gets the index of the previous trip that has a stop before (or at) beforeTime at 
     * the stop at stopIndex 
     */
    public TripTimes getPreviousTrip(int stopIndex, int beforeTime, boolean haveBicycle, 
            RoutingRequest options) {
        boolean pickup = false;
        int mask = pickup ? MASK_PICKUP : MASK_DROPOFF;
        int shift = pickup ? SHIFT_PICKUP : SHIFT_DROPOFF;
        if ((perStopFlags[stopIndex + 1] & mask) >> shift == NO_PICKUP) {
            return null;
        }
        boolean wheelchair = options.wheelchairAccessible;
        if (wheelchair && (perStopFlags[stopIndex + 1] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return null;
        }
        // linear search:
        // because trips may change with stoptime updates, we cannot count on them being sorted
        TripTimes bestTrip = null;
        int bestTime = Integer.MIN_VALUE;
        for (int i = 0; i < trips.size(); i++) {
            // grab a reference before tests in case it is swapped out by an update thread
            TripTimes currTrip = tripTimes.get(i); 
            int currTime = currTrip.getArrivalTime(stopIndex);
            if (currTime <= beforeTime && currTime > bestTime && 
                    tripAcceptable(currTrip.trip, haveBicycle, wheelchair) &&
                    ! options.bannedTrips.contains(trips.get(i).getId())) {
                bestTrip = currTrip;
                bestTime = currTime;
            }
        }
        return bestTrip;
    }
    
    public List<Stop> getStops() {
        return Arrays.asList(stops);
    }

    /** Gets the departure time for a given stop on a given trip */
    public int getDepartureTime(int hop, int trip) {
        return tripTimes.get(trip).getDepartureTime(hop);
    }

    /** Gets the arrival time for a given HOP on a given trip */
    public int getArrivalTime(int hop, int trip) {
        return tripTimes.get(trip).getArrivalTime(hop);
    }

    /** Gets the running time after a given stop (i.e. for the given HOP) on a given trip */
    public int getRunningTime(int stopIndex, int trip) {
        return tripTimes.get(trip).getRunningTime(stopIndex);
    }

    /** Gets the dwell time at a given stop (i.e. before then given HOP) on a given trip */
    public int getDwellTime(int hop, int trip) {
        // the dwell time of a hop is the dwell time *before* that hop.
        return tripTimes.get(trip).getDwellTime(hop);
    }

    // SEEMS UNUSED
    public boolean getWheelchairAccessible(int stopIndex, int trip) {
//        if ((perStopFlags[stopIndex] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
//            return false;
//        }
//        if ((perTripFlags.get(trip) & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
//            return false;
//        }
        return true;
    }

    // SEEMS UNUSED
    public boolean getBikesAllowed(int trip) {
//        return (perTripFlags.get(trip) & FLAG_BIKES_ALLOWED) != 0;
        return true;
    }

    /** Gets the Trip object for a given trip index */
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
    
    public int getTripIndex(AgencyAndId tripId) {
        int ret = 0;
        for (Trip t : trips) {
            if (t.getId().equals(tripId)) // replace with indexing in stoptime updater?
                return ret;
            ret += 1;
        }
        return -1;
    }

    /** Returns whether passengers can alight at a given stop */
    public boolean canAlight(int stopIndex) {
        return getAlightType(stopIndex) != NO_PICKUP;
    }

    /** Returns whether passengers can board at a given stop */
    public boolean canBoard(int stopIndex) {
        return getBoardType(stopIndex) != NO_PICKUP;
    }

    /** Returns the zone of a given stop */
    public String getZone(int stopIndex) {
        //return zones[stopIndex];
        return stops[stopIndex].getZoneId();
    }

    /** Returns an arbitrary trip that uses this pattern */
    public Trip getExemplar() {
        return exemplar;
    }

    /** Returns the shortest possible running time for this stop */
    public int getBestRunningTime(int stopIndex) {
        return bestRunningTimes[stopIndex];
    }

    /** Returns the shortest possible dwell time at this stop */
    public int getBestDwellTime(int stopIndex) {
        if (bestDwellTimes == null) {
            return 0;
        }
        return bestDwellTimes[stopIndex];
    }

    /** The current headsign as-of this stop if it differs from the trip headsign or null otherwise */
    public String getHeadsign(int stopIndex, int trip) {
        if (headsigns == null)
            return null;
        List<String> headsignsForTrip = headsigns.get(trip);
        if (headsignsForTrip == null)
            return null;
        return headsignsForTrip.get(stopIndex);
    }

    public int getAlightType(int stopIndex) {
        return (perStopFlags[stopIndex] & MASK_DROPOFF) >> SHIFT_DROPOFF;
    }

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
    
    /** Gets all the departure times at a given stop (not used in routing) */
    public Iterator<Integer> getDepartureTimes(int stopIndex) {
        return new DeparturesIterator(stopIndex);
    }

    public TripTimes getTripTimes(int tripIndex) {
        return tripTimes.get(tripIndex);
    }

    public void update(UpdateList ul) {
        int tripIndex = this.getTripIndex(ul.tripId);
        if (tripIndex == -1) {
            LOG.debug("tripId {} not found", ul.tripId);
            return;
        }
        int stopIndex = ul.findUpdateStopIndex(this);
        if (stopIndex == -1) {
            LOG.debug("update block did not match stopIds");
            return;
        }
        TripTimes oldTimes = getTripTimes(tripIndex);
        TripTimes newTimes = oldTimes.updatedClone(ul, stopIndex);
        this.tripTimes.set(tripIndex, newTimes);
    }
    
    public void addTrip(Trip trip, List<StopTime> stopTimes) {
        // TODO: double-check that the stops and pickup/dropoffs are right for this trip
        int nextIndex = tripTimes.size();
        tripTimes.add(new TripTimes(trip, nextIndex, stopTimes));
        trips.add(trip);
        
        // stoptimes can have headsign info that overrides the trip's headsign
        ArrayList<String> headsigns = new ArrayList<String>();
        boolean allHeadsignsNull = true;
        for (StopTime st : stopTimes) {
            String headsign = st.getStopHeadsign();
            if (headsign != null)
                allHeadsignsNull = false;
            headsigns.add(headsign);
        }
        if (allHeadsignsNull)
            headsigns = null;
        this.headsigns.add(headsigns);
        // stoptimes should be transposed later and compacted with reused arrays
        // 1x1 array should always return the same headsign to allow for no change 
    }

    public boolean allDwellsZero(int hopIndex) {
        for (int t = 0; t < trips.size(); ++t) {
            if (getDwellTime(hopIndex, t) != 0) {
                return false;
            }
        }
        return true;
    }
    
//    
//    if (headsigns != null) { 
//        // DO NOT transpose headsigns to allow reusing rows
//        this.headsigns = new String[nHops][nTrips]; 
//        String[] nullRow = null; 
//        // headsigns contains 1 less headsign than there are stops, because sign change is pointless at the last stop
//        for (int s = 0; s < nHops; ++s) { 
//            boolean rowIsNull = true;
//            for (int t = 0; t < headsigns[s].size(); ++t) {
//                this.headsigns[s][t] = headsigns[s].get(t); 
//                if (this.headsigns[s][t] != null) {
//                    rowIsNull = false; 
//                }
//            }
//            if (rowIsNull) {
//                // repeat the same row object when empty row encountered
//                if (nullRow == null) {
//                    nullRow = this.headsigns[s];
//                } else {
//                    this.headsigns[s] = nullRow;
//                }
//            }
//        }
//    }


    public int getServiceId() { 
        return serviceId;
    }

}
