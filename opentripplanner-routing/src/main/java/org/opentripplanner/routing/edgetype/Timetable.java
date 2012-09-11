package org.opentripplanner.routing.edgetype;

import static org.opentripplanner.routing.edgetype.TableTripPattern.FLAG_WHEELCHAIR_ACCESSIBLE;
import static org.opentripplanner.routing.edgetype.TableTripPattern.MASK_DROPOFF;
import static org.opentripplanner.routing.edgetype.TableTripPattern.NO_PICKUP;
import static org.opentripplanner.routing.edgetype.TableTripPattern.SHIFT_DROPOFF;
import static org.opentripplanner.routing.edgetype.TableTripPattern.SHIFT_PICKUP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.trippattern.CanceledTripTimes;
import org.opentripplanner.routing.trippattern.DecayingDelayTripTimes;
import org.opentripplanner.routing.trippattern.ScheduledTripTimes;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.trippattern.UpdateBlock;
import org.opentripplanner.routing.trippattern.UpdatedTripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * Timetables provide most of the TripPattern functionality. Each TripPattern may possess more 
 * than one Timetable when stop time updates are being applied: one for the scheduled stop times, 
 * one for each snapshot of updated stop times, another for a working buffer of updated stop 
 * times, etc. Timetable is a non-static nested (inner) class, so each Timetable belongs to a 
 * specific TripPattern, whose fields it can access.
 */
// TODO: Timetable is not large enough that it should probably be split out and have an explicit
// reference to its owning pattern via a field. However it is super convenient to have access
// to the trippattern's fields.
public class Timetable implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(Timetable.class);

    /** 
     * The Timetable size (number of TripTimes) at which indexes will be built for all stops. 
     * Below this size, departure and arrival times will be found by linear search. Above this
     * size, it will be possible to use binary search.
     */
    private static final int INDEX_THRESHOLD = 16;

    private final TableTripPattern pattern;
    
    /** 
     * Contains one TripTimes object for each scheduled trip (even cancelled ones) and possibly
     * additional TripTimes objects for unscheduled trips.
     */
    private final ArrayList<TripTimes> tripTimes;

    /** 
     * If the departures index is null, this timetable has not been indexed: use a linear search. 
     * Unfortunately you really do need 2 indexes, because dwell times for different trips at
     * the same stop may overlap. The indexes always contain the same elements as the main
     * tripTimes List, but are re-sorted at each stop to allow binary searches.
     */
    private transient TripTimes[][] arrivalsIndex = null;
    private transient TripTimes[][] departuresIndex = null;

    /** For each hop, the best running time. This serves to provide lower bounds on traversal time.
     * TODO: should be indexed per timetable, not in patterns, and be transient. */
    private transient int bestRunningTimes[];
    
    /** For each stop, the best dwell time. This serves to provide lower bounds on traversal time. */
    private transient int bestDwellTimes[];

    /** Construct an empty Timetable. */
    public Timetable(TableTripPattern pattern) {
        tripTimes = new ArrayList<TripTimes>();
        this.pattern = pattern;
    }
    
    /** 
     * Copy constructor: create an un-indexed Timetable with the same TripTimes as the 
     * specified timetable. 
     */
    private Timetable (Timetable tt) {
        tripTimes = new ArrayList<TripTimes>(tt.tripTimes);
        this.pattern = tt.pattern;
    }
    
    /** 
     * This copy instance method can see the enclosing TripPattern instance, while the copy 
     * constructor does not. The only publicly visible way to make a timetable, and it should
     * probably be protected.
     */
    public Timetable copy() {
        return new Timetable(this);
    }
    
    /**
     * Produces 2D index arrays that are stop-major and sorted, allowing binary search at any 
     * given stop. It is of course inefficient to call this after updating only one or two 
     * trips in a pattern since we can usually get by with swapping only the new trip into the 
     * existing already-sorted lists. But let's see realistically how resource-intensive this 
     * is before optimizing it.
     */
    private void index() {
        int nHops = pattern.stops.length - 1;
        arrivalsIndex = new TripTimes[nHops][];
        departuresIndex = new TripTimes[nHops][];
        boolean departuresFifo = true;
        boolean arrivalsMatchDepartures = true;
        for (int hop = 0; hop < nHops; hop++) {
            // copy canonical TripTimes List into new arrays
            arrivalsIndex[hop] = tripTimes.toArray(new TripTimes[tripTimes.size()]);
            departuresIndex[hop] = tripTimes.toArray(new TripTimes[tripTimes.size()]);
            // TODO: STOP VS HOP
            Arrays.sort(arrivalsIndex[hop], new TripTimes.ArrivalsComparator(hop));
            Arrays.sort(departuresIndex[hop], new TripTimes.DeparturesComparator(hop));
            if (hop > 0 && Arrays.equals(departuresIndex[hop], departuresIndex[hop - 1]))
                departuresIndex[hop] = departuresIndex[hop - 1];
            else
                departuresFifo = false;
            if (Arrays.equals(departuresIndex[hop], arrivalsIndex[hop]))
                arrivalsIndex[hop] = departuresIndex[hop];
            else
                arrivalsMatchDepartures = false;
        }
        if (departuresFifo) {
            //LOG.debug("Compressing FIFO Timetable index.");
            departuresIndex = Arrays.copyOf(departuresIndex, 1);
        }
        if (arrivalsMatchDepartures) {
            //LOG.debug("Reusing departures index where arrivals index is identical.");
            arrivalsIndex = departuresIndex;
        }
    }
    
    /** 
     * Get the index of the next (previous) trip that departs (arrives) from the specified stop 
     * at or after (before) the specified time. The haveBicycle parameter must be passed in 
     * because we cannot determine whether the user is in possession of a rented bicycle from 
     * the options alone.
     */
    protected TripTimes getNextTrip(int stopIndex, int time, boolean haveBicycle,
            RoutingRequest options, boolean boarding) {
        int mask = boarding ? TableTripPattern.MASK_PICKUP : MASK_DROPOFF;
        int shift = boarding ? SHIFT_PICKUP : SHIFT_DROPOFF;
        int stopOffset = boarding ? 0 : 1;
        if ((pattern.perStopFlags[stopIndex + stopOffset] & mask) >> shift == NO_PICKUP) {
            return null;
        }
        if (options.wheelchairAccessible && 
                (pattern.perStopFlags[stopIndex + stopOffset] & FLAG_WHEELCHAIR_ACCESSIBLE) == 0) {
            return null;
        }
        TripTimes[][] tableIndex = boarding ? departuresIndex : arrivalsIndex; 
        // binary search if this timetable has been indexed
        // TODO: potential optimization: when indexing, check if new sorted trip arrays are the 
        // same as one for previous stop, and reuse them.
        // If they are all the same, trip is FIFO and needs no index (ie tripTimes can be used
        // as index at every stop). 
        if (tableIndex != null) { 
            // grab the sorted list of TripTimes for this particular stop
            TripTimes[] sorted;
            if (tableIndex.length == 1) // for optimized FIFO patterns
                sorted = departuresIndex[0];
            else
                sorted = tableIndex[stopIndex];
            int tripIndex;
            if (boarding)
                tripIndex = TripTimes.binarySearchDepartures(sorted, stopIndex, time); 
            else
                tripIndex = TripTimes.binarySearchArrivals(sorted, stopIndex, time); 
            // an alternative to conditional increment/decrement would be to sort the arrivals
            // index in decreasing order, but that would require changing the search algorithm 
            int increment = boarding ? 1 : -1;
            int terminate = boarding ? sorted.length : -1;
            //these appear to actually be hop indexes, which is what the binary search accepts
            while (tripIndex != terminate) { 
                TripTimes tt = sorted[tripIndex];
                Trip t = tt.getTrip();
                if (tripAcceptable(t, haveBicycle, options)) {
                    return tt;
                }
                tripIndex += increment;
            }
            return null;
        }
        // no index. fall through to linear search:
        // because trips may change with stoptime updates, we cannot count on them being sorted
        TripTimes bestTrip = null;
        int bestTime = Integer.MAX_VALUE;
        if ( ! boarding) // reverse direction of inequalities by negating times
            time = -time;
        for (int i = 0; i < tripTimes.size(); i++) {
            TripTimes currTrip = tripTimes.get(i); 
            int currTime;
            if (boarding) 
                currTime = currTrip.getDepartureTime(stopIndex);
            else // reverse direction of inequalities by negating times
                currTime = -(currTrip.getArrivalTime(stopIndex));
            if (currTime >= time && currTime < bestTime && 
                    tripAcceptable(currTrip.getTrip(), haveBicycle, options)) {
                bestTrip = currTrip;
                bestTime = currTime;
            }
        }
        return bestTrip;
    }
    
    /** Gets the departure time for a given hop on a given trip */
    public int getDepartureTime(int hop, int trip) {
        return tripTimes.get(trip).getDepartureTime(hop);
    }

    /** Gets the arrival time for a given hop on a given trip */
    public int getArrivalTime(int hop, int trip) {
        return tripTimes.get(trip).getArrivalTime(hop);
    }

    /** Gets the running time after a given stop (i.e. for the given hop) on a given trip */
    public int getRunningTime(int stopIndex, int trip) {
        return tripTimes.get(trip).getRunningTime(stopIndex);
    }

    /** Gets the dwell time at a given stop (i.e. before then given hop) on a given trip */
    public int getDwellTime(int hop, int trip) {
        // the dwell time of a hop is the dwell time *before* that hop.
        return tripTimes.get(trip).getDwellTime(hop);
    }

    /**
     * Finish off a TripPattern once all TripTimes have been added to it. This involves caching
     * lower bounds on the running times and dwell times at each stop, and may perform other
     * actions to compact the data structure such as trimming and deduplicating arrays.
     */
    public void finish() {
        int nHops = pattern.stops.length - 1;
        int nTrips = tripTimes.size();
        // TODO: bestRunningTimes is specific to the _updated_ times and should be moved into 
        // the inner class
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
        
        // analyze();
        // compact();
        // index();
        
        // break even list size for linear and binary searches was determined to be around 16
        if (nTrips > INDEX_THRESHOLD) {
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
            return nextPosition < tripTimes.size();
        }

        @Override
        public Integer next() {
            return tripTimes.get(nextPosition++).getDepartureTime(stopIndex);
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
    public int getTripIndex(AgencyAndId tripId) {
        int ret = 0;
        for (TripTimes tt : tripTimes) {
            // could replace linear search with indexing in stoptime updater, but not necessary
            // at this point since the updater thread is far from pegged.
            if (tt.getTrip().getId().equals(tripId)) 
                return ret;
            ret += 1;
        }
        return -1;
    }
    
    /** 
     * Not private because it's used when traversing interline dwells, which refer to order
     * in the scheduled trip pattern. 
     */
    public TripTimes getTripTimes(int tripIndex) {
        return tripTimes.get(tripIndex);
    }

    /**
     * Apply the UpdateBlock to the appropriate ScheduledTripTimes from this Timetable. 
     * The existing TripTimes must not be modified directly because they may be shared with 
     * the underlying scheduledTimetable, or other updated Timetables.
     * The StoptimeUpdater performs the protective copying of this Timetable. It is not done in 
     * this update method to avoid repeatedly cloning the same Timetable when several updates 
     * are applied to it at once.
     * @return whether or not the timetable actually changed as a result of this operation
     * (maybe it should do the cloning and return the new timetable to enforce copy-on-write?) 
     */
    public boolean update(UpdateBlock block) {
        try {
             // Though all timetables have the same trip ordering, some may have extra trips due to 
             // the dynamic addition of unscheduled trips.
             // However, we want to apply trip update blocks on top of *scheduled* times 
            int tripIndex = getTripIndex(block.tripId);
            if (tripIndex == -1) {
                LOG.info("tripId {} not found in pattern.", block.tripId);
                return false;
            } else {
                LOG.trace("tripId {} found at index {} (in scheduled timetable)", block.tripId, tripIndex);
            }
            // 'stop' Index as in transit stop (not 'end', not 'hop')
            int stopIndex = block.findUpdateStopIndex(pattern);
            if (stopIndex == UpdateBlock.MATCH_FAILED) {
                LOG.warn("Unable to match update block to stopIds.");
                return false;
            }
            TripTimes existingTimes = getTripTimes(tripIndex);
            ScheduledTripTimes scheduledTimes = existingTimes.getScheduledTripTimes();
            TripTimes newTimes;
            if (block.isCancellation()) {
                newTimes = new CanceledTripTimes(scheduledTimes);
            } 
            else {
                newTimes = new UpdatedTripTimes(scheduledTimes, block, stopIndex);
                if ( ! newTimes.timesIncreasing()) {
                    LOG.warn("Resulting UpdatedTripTimes has non-increasing times. " +
                             "Falling back on DecayingDelayTripTimes.");
                    LOG.warn(block.toString());
                    LOG.warn(newTimes.toString());
                    int delay = newTimes.getDepartureDelay(stopIndex);
                    // maybe decay should be applied on top of the update (wrap Updated in Decaying), 
                    // starting at the end of the update block
                    newTimes = new DecayingDelayTripTimes(scheduledTimes, stopIndex, delay);
                    LOG.warn(newTimes.toString());
                    if ( ! newTimes.timesIncreasing()) {
                        LOG.error("Even these trip times are non-increasing. Underlying schedule problem?");
                        return false;
                    }
                }
            }
            // Update succeeded, save the new TripTimes back into this Timetable.
            this.tripTimes.set(tripIndex, newTimes);
            return true;
        } catch (Exception e) { // prevent server from dying while debugging
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Add a trip to this Timetable. The Timetable must be analyzed, compacted, and indexed
     * any time trips are added, but this is not done automatically because it is time consuming
     * and should only be done once after an entire batch of trips are added.
     * Any new trip that is added is a ScheduledTripTimes. The scheduledTimetable will then 
     * contain only ScheduledTripTimes, and any updated Timetables will contain TripTimes
     * that wrap these ScheduledTripTimes, plus any additional trips as ScheduledTripTimes.
     * Maybe subclass ScheduledTripTimes with an equivalent ExtraTripTimes class to make this 
     * distinction clear.
     */
    public void addTrip(Trip trip, List<StopTime> stopTimes) {
        // TODO: double-check that the stops and pickup/dropoffs are right for this trip
        tripTimes.add(new ScheduledTripTimes(trip, stopTimes));
        // TODO eliminate delegation / encapsulation fail
        pattern.trips.add(trip);
    }

    /** 
     * Check that all dwell times at the given stop are zero, which allows removing the dwell edge. 
     */
    boolean allDwellsZero(int hopIndex) {
        for (int t = 0; t < tripTimes.size(); ++t) {
            if (getDwellTime(hopIndex, t) != 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Once a trip has been found departing or arriving at an appropriate time, check whether that 
     * trip fits other restrictive search criteria such as bicycle and wheelchair accessibility.
     * 
     * GTFS bike extensions based on mailing list message at: 
     * https://groups.google.com/d/msg/gtfs-changes/QqaGOuNmG7o/xyqORy-T4y0J
     * 2: bikes allowed
     * 1: no bikes allowed
     * 0: no information (same as field omitted)
     * 
     * If route OR trip explicitly allows bikes, bikes are allowed.
     * 
     * TODO move into tripTimes class
     */
    private static boolean tripAcceptable(Trip trip, boolean bicycle, RoutingRequest options) {
        if (options.bannedTrips.contains(trip.getId()))
            return false;
        if (options.wheelchairAccessible && trip.getWheelchairAccessible() != 1)
            return false;
        if (bicycle)
            if ((trip.getTripBikesAllowed() != 2) &&    // trip does not explicitly allow bikes and
                (trip.getRoute().getBikesAllowed() != 2 // route does not explicitly allow bikes or  
                || trip.getTripBikesAllowed() == 1))    // trip explicitly forbids bikes
                return false; 
        return true;
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

} 

