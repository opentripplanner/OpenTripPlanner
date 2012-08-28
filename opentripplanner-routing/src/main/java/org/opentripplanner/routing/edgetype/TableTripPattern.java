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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import lombok.Delegate;

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

    /** 
     * This timetable holds the 'official' stop times from GTFS. If realtime stoptime updates are 
     * applied, trips searches will be conducted using another timetable and this one will serve to 
     * find early/late offsets, or as a fallback if the other timetable becomes corrupted or
     * expires. 
     * Via Lombok Delegate, calling timetable methods on a TableTripPattern will call them on its
     * scheduled timetable.
     */
    @Delegate
    protected final Timetable scheduledTimetable = new Timetable();

//    @XmlElement
//    private final ArrayList<Integer> perTripFlags = new ArrayList<Integer>();

    // redundant since tripTimes have a trip
    // however it's nice to have for order reference, since all timetables must have tripTimes
    // in this order, e.g. for interlining. 
    // potential optimization: trip fields can be removed from TripTimes?
    /**
     * This pattern may have multiple Timetable objects, but they should all contain TripTimes
     * for the same trips, in the same order (that of the scheduled Timetable). An exception to 
     * this rule may arise if unscheduled trips are added to a Timetable. For that case we need 
     * to search for trips/TripIds in the Timetable rather than the enclosing TripPattern.  
     */
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

    private Boolean tripAcceptable(Trip trip, boolean bicycle, boolean wheelchair) {
        boolean result = true;
        if (wheelchair)
            result &= trip.getWheelchairAccessible() == 1;
        if (bicycle)
            result &= trip.getTripBikesAllowed() == 2 ||
            (trip.getRoute().getBikesAllowed() == 2 && trip.getTripBikesAllowed() != 1); 
        return result;
    }
    
    public List<Stop> getStops() {
        return Arrays.asList(stops);
    }

    /*
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
     */
    
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
    
    private void writeObject(ObjectOutputStream outputStream) throws ClassNotFoundException,
            IOException {
        finish();
        outputStream.defaultWriteObject();
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

    public int getNumTrips () {
        return trips.size();
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
    
        
    public TripTimes getNextTrip(int stopIndex, int afterTime, boolean haveBicycle,
            RoutingRequest options) {
        Timetable timetable;
        TimetableSnapshot snapshot = options.rctx.timetableSnapshot; 
        if (snapshot != null)
            timetable = snapshot.resolve(this);
        else
            timetable = scheduledTimetable;
        return timetable.getNextTrip(stopIndex, afterTime, haveBicycle, options);
    }
    
    public TripTimes getPreviousTrip(int stopIndex, int beforeTime, boolean haveBicycle, 
            RoutingRequest options) {
        Timetable timetable;
        TimetableSnapshot snapshot = options.rctx.timetableSnapshot; 
        if (snapshot != null)
            timetable = snapshot.resolve(this);
        else
            timetable = scheduledTimetable;
        return timetable.getPreviousTrip(stopIndex, beforeTime, haveBicycle, options);
    }        

    /* NESTED CLASS */
    
    /** 
     * Timetables provide most of the trippattern functionality. Each trip pattern may need more 
     * than one if stop time updates are being applied (one for the scheduled stop times, one for
     * the updated stop times, another for the working buffer of updated stoptimes, etc. 
     * This is a non-static nested (inner) class so each timetable belongs to a specific 
     * trippattern, whose fields it can access.
     */
    public class Timetable implements Serializable {
        
        private static final long serialVersionUID = 1L;

        private final ArrayList<TripTimes> tripTimes;

        /** if the index is null, this timetable has not been indexed. use a linear search. */
        private TripTimes[][] arrivalsIndex;
        private TripTimes[][] departuresIndex;
        
        public Timetable() {
            tripTimes = new ArrayList<TripTimes>();
        }
        
        /** copy constructor */
        private Timetable (Timetable tt) {
            tripTimes = new ArrayList<TripTimes>(tt.tripTimes);
        }
        
        /** copy instance method sees enclosing instance */
        public Timetable copy() {
            return new Timetable(this);
        }
        
        /* It is of course inefficient to call this after updating only one or two trips in a 
         * pattern, since we can potentially get by with swapping only the new trip
         * into the already-sorted lists. But let's see realistically how resource-intensive
         * this is before optimizing it.
         */
        private void index() {
            int nHops = stops.length - 1;
            // index is stop-major and sorted, allowing binary search at a given stop
            arrivalsIndex = new TripTimes[nHops][];
            departuresIndex = new TripTimes[nHops][];
            for (int hop = 0; hop < nHops; hop++) {
                // copy arraylist into new arrays
                arrivalsIndex[hop] = tripTimes.toArray(new TripTimes[tripTimes.size()]);
                departuresIndex[hop] = tripTimes.toArray(new TripTimes[tripTimes.size()]);
                // TODO: STOP VS HOP
                Arrays.sort(arrivalsIndex[hop], TripTimes.getArrivalsComparator(hop));
                Arrays.sort(departuresIndex[hop], TripTimes.getDeparturesComparator(hop));
            }
        }
        
        /** 
         * Get the index of the next trip that has a stop after (or at) 
         * afterTime at the stop at stopIndex. 
         * The haveBicycle parameter must be passed in because we cannot determine whether the user
         * is in possession of a rented bicycle from the options alone.
         */
        // Method is protected so Lombok won't delegate to it.
        protected TripTimes getNextTrip(int stopIndex, int afterTime, boolean haveBicycle,
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
            // binary search if this timetable has been indexed
            // TODO: potential optimization: when indexing, check if new sorted trip arrays are the 
            // same as one for previous stop, and reuse them.
            // If they are all the same, trip is FIFO and needs no index (ie tripTimes can be used
            // as index at every stop). 
            if (departuresIndex != null) { 
                // grab the sorted list of TripTimes for this particular stop
                // if (departuresIndex.length == 1) // for optimized FIFO patterns
                //     index = departuresIndex[0];
                // else
                TripTimes[] index = departuresIndex[stopIndex];
                int tripIndex = TripTimes.binarySearchDepartures(index, stopIndex, afterTime); 
                //these appear to actually be hop indexes, which is what the binary search accepts
                while (tripIndex < index.length) {
                    TripTimes tt = index[tripIndex];
                    Trip t = tt.trip;
                    if (tripAcceptable(t, haveBicycle, wheelchair) && 
                        !options.bannedTrips.contains(t.getId())) {
                        return tt;
                    }
                    tripIndex += 1;
                }
                return null;
            }
            // no index. fall through to linear search:
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
         * the stop at stopIndex.
         */
        // Method is protected so Lombok won't delegate to it.
        // TODO could be merged with departure search, lots of duplicate code
        protected TripTimes getPreviousTrip(int stopIndex, int beforeTime, boolean haveBicycle, 
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
            // binary search if this timetable has been indexed
            if (arrivalsIndex != null) {
                // search through the sorted list of TripTimes for this particular stop
                TripTimes[] index = arrivalsIndex[stopIndex];
                int tripIndex = TripTimes.binarySearchArrivals(index, stopIndex, beforeTime); 
                //these appear to actually be hop indexes, which is what the binary search accepts
                while (tripIndex >= 0) {
                    TripTimes tt = index[tripIndex];
                    Trip t = tt.trip;
                    if (tripAcceptable(t, haveBicycle, wheelchair) && 
                        !options.bannedTrips.contains(t.getId())) {
                        return tt;
                    }
                    tripIndex -= 1;
                }
                return null;
            }
            // no index. fall through to linear search:
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

        // finish off the pattern once all times have been added 
        // cache best (lowest) running times and dwell times; maybe trim arrays too
        public void finish() {
            int nHops = stops.length - 1;
            int nTrips = trips.size();
            // TODO: bestRunningTimes is specific to the _updated_ times and should be moved into the inner class
            bestRunningTimes = new int[nHops];
            boolean nullArrivals = false; // TODO: should scan through triptimes?
            if ( ! nullArrivals) {
                bestDwellTimes = new int[nHops];
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
            // break even list size for linear and binary searches was determined to be around 16
            if (nTrips > 16) {
                //LOG.debug("indexing pattern with {} trips", nTrips);
                index(); 
            } else {
                arrivalsIndex = null;
                departuresIndex = null;
            }
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

        /** @return the index of TripTimes for this Trip(Id) in this particular Timetable */
        private int getTripIndex(AgencyAndId tripId) {
            int ret = 0;
            for (TripTimes tt : tripTimes) {
                if (tt.trip.getId().equals(tripId)) // replace with indexing in stoptime updater?
                    return ret;
                ret += 1;
            }
            return -1;
        }
        
        /** not private because it's used when traversing interline dwells, which refer to order
         * in the scheduled trip pattern. */
        public TripTimes getTripTimes(int tripIndex) {
            return tripTimes.get(tripIndex);
        }

        public boolean update(UpdateList ul) {
            /* though all timetables have the same trip ordering, some may have extra trips due to 
             * the dynamic addition of unscheduled trips */
            int tripIndex = getTripIndex(ul.tripId);
            if (tripIndex == -1) {
                LOG.trace("tripId {} not found", ul.tripId);
                return false;
            } else {
                LOG.trace("tripId {} found at index {}", ul.tripId, tripIndex);
            }
            int stopIndex = ul.findUpdateStopIndex(TableTripPattern.this);
            if (stopIndex == -1) {
                LOG.trace("update block did not match stopIds");
                return false;
            }
            TripTimes oldTimes = getTripTimes(tripIndex);
            TripTimes newTimes = oldTimes.updatedClone(ul, stopIndex);
            this.tripTimes.set(tripIndex, newTimes);
            return true;
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
            TableTripPattern.this.headsigns.add(headsigns);
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
                
    } // END Class Timetable

    public Iterator<Integer> getScheduledDepartureTimes(int stopIndex) {
        return scheduledTimetable.getDepartureTimes(stopIndex);
    }
    
}
