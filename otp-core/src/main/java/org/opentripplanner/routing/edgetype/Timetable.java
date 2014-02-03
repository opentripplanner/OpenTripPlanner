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
import java.util.TimeZone;

import lombok.Getter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;


/**
 * Timetables provide most of the TripPattern functionality. Each TripPattern may possess more than
 * one Timetable when stop time updates are being applied: one for the scheduled stop times, one for
 * each snapshot of updated stop times, another for a working buffer of updated stop times, etc.
 */
public class Timetable implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(Timetable.class);

    /**
     * The Timetable size (number of TripTimes) at which indexes will be built for all stops.
     * Below this size, departure and arrival times will be found by linear search. Above this
     * size, it will be possible to use binary search.
     * Break even list size for linear and binary searches was determined to be around 16.
     */
    private static final int INDEX_THRESHOLD = 16;

    /**
     * This creates a circular reference between trippatterns and timetables.
     * Be careful during serialization.
     */
    private final TableTripPattern pattern;

    /**
     * Contains one TripTimes object for each scheduled trip (even cancelled ones) and possibly
     * additional TripTimes objects for unscheduled trips.
     */
    private final ArrayList<TripTimes> tripTimes;

    /**
     * The ServiceDate for which this (updated) timetables is valid.
     * If null, then it is valid for all dates.
     */
    @Getter
    private final ServiceDate serviceDate;

    /**
     * If the departures index is null, this timetable has not been indexed: use a linear search.
     * Unfortunately you really do need 2 indexes, because dwell times for different trips at
     * the same stop may overlap. The indexes always contain the same elements as the main
     * tripTimes List, but are re-sorted at each stop to allow binary searches.
     */
    private transient TripTimes[][] arrivalsIndex = null;
    private transient TripTimes[][] departuresIndex = null;

    /**
     * For each hop, the best running time. This serves to provide lower bounds on traversal time.
     */
    private transient int bestRunningTimes[];

    /**
     * For each stop, the best dwell time. This serves to provide lower bounds on traversal time.
     */
    private transient int bestDwellTimes[];

    /**
     * Helps determine whether a particular pattern is worth searching for departures at a given time.
     */
    private transient int minDepart, maxArrive;

    /** Construct an empty Timetable. */
    public Timetable(TableTripPattern pattern) {
        tripTimes = new ArrayList<TripTimes>();
        this.pattern = pattern;
        this.serviceDate = null;
    }

    /**
     * Copy constructor: create an un-indexed Timetable with the same TripTimes as the
     * specified timetable.
     */
    Timetable (Timetable tt, ServiceDate serviceDate) {
        tripTimes = new ArrayList<TripTimes>(tt.tripTimes);
        this.serviceDate = serviceDate;
        this.pattern = tt.pattern;
    }

    public int getStopSequence(int stopIndex, int tripIndex) {
        return tripTimes.get(tripIndex).getStopSequence(stopIndex);
    }

    /**
     * Produces 2D index arrays that are stop-major and sorted, allowing binary search at any
     * given stop. It is of course inefficient to call this after updating only one or two
     * trips in a pattern since we can usually get by with swapping only the new trip into the
     * existing already-sorted lists. But let's see realistically how resource-intensive this
     * is before optimizing it.
     */
    private void index() {
        int nHops = pattern.getHopCount();
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
            if (hop > 0) {
                if (Arrays.equals(departuresIndex[hop], departuresIndex[hop - 1])) {
                    departuresIndex[hop] = departuresIndex[hop - 1];
                } else {
                    departuresFifo = false;
                }
            }
            if (Arrays.equals(departuresIndex[hop], arrivalsIndex[hop])) {
                arrivalsIndex[hop] = departuresIndex[hop];
            } else {
                arrivalsMatchDepartures = false;
            }
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
     * Get the next (previous) trip that departs (arrives) from the specified stop at or after
     * (before) the specified time. The haveBicycle parameter must be passed in because we cannot
     * determine whether the user is in possession of a rented bicycle from the options alone. If a
     * pre-allocated array is passed in via the optional adjacentTimes parameter, that array will be
     * filled with the main result plus a suitable number of TripTimes roughly temporally adjacent
     * to the main result. If the main result is null, the contents of the adjacentTimes array are
     * undefined. Note that no guarantees of exhaustiveness, contiguity, etc. are made about the
     * additional TripTimes objects.
     *
     * @return the TripTimes object representing the (possibly updated) best trip, or null if no
     * trip matches both the time and other criteria.
     */
    protected TripTimes getNextTrip(int stopIndex, int time, State state0, ServiceDay sd,
            boolean haveBicycle, boolean boarding) {
        TripTimes bestTrip = null;
        int index;
        TripTimes[][] tableIndex = boarding ? departuresIndex : arrivalsIndex;
        Stop currentStop = pattern.getStop(stopIndex);
        if (tableIndex != null) {
            TripTimes[] sorted;
            // this timetable has been indexed, use binary search
            if (tableIndex.length == 1) { // for optimized FIFO patterns
                sorted = tableIndex[0];
            } else {
                sorted = tableIndex[boarding ? stopIndex : stopIndex - 1];
            }
            // an alternative to conditional increment/decrement would be to sort the arrivals
            // index in decreasing order, but that would require changing the search algorithm
            if (boarding) {
                index = TripTimes.binarySearchDepartures(sorted, stopIndex, time);
                while (index < sorted.length) {
                    TripTimes tt = sorted[index++];
                    if (tt.tripAcceptable(state0,
                            currentStop, sd, haveBicycle, stopIndex, boarding)) {
                        bestTrip = tt;
                        break;
                    }
                }
            } else {
                index = TripTimes.binarySearchArrivals(sorted, stopIndex - 1, time);
                while (index >= 0) {
                    TripTimes tt = sorted[index--];
                    if (tt.tripAcceptable(state0,
                            currentStop, sd, haveBicycle, stopIndex, boarding)) {
                        bestTrip = tt;
                        break;
                    }
                }
            }
        } else {
            // no index present on this timetable. use a linear search:
            // because trips may change with stoptime updates, we cannot count on them being sorted
            int bestTime = boarding ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            for (TripTimes tt : tripTimes) {
                // hoping JVM JIT will distribute the loop over the if clauses as needed
                if (boarding) {
                    int depTime = tt.getDepartureTime(stopIndex);
                    if (depTime >= time && depTime < bestTime && tt.tripAcceptable(state0,
                            currentStop, sd, haveBicycle, stopIndex, boarding)) {
                        bestTrip = tt;
                        bestTime = depTime;
                    }
                } else {
                    int arvTime = tt.getArrivalTime(stopIndex - 1);
                    if (arvTime <= time && arvTime > bestTime && tt.tripAcceptable(state0,
                            currentStop, sd, haveBicycle, stopIndex, boarding)) {
                        bestTrip = tt;
                        bestTime = arvTime;
                    }
                }
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
        int nHops = pattern.getHopCount();
        int nTrips = tripTimes.size();
        bestRunningTimes = new int[nHops];
        boolean nullArrivals = false; // TODO: should scan through triptimes?
        if ( ! nullArrivals) {
            bestDwellTimes = new int[nHops];
            for (int h = 1; h < nHops; ++h) { // dwell time is undefined on first hop
                bestDwellTimes[h] = Integer.MAX_VALUE;
                for (int t = 0; t < nTrips; ++t) {
                    int dt = this.getDwellTime(h, t);
                    if (bestDwellTimes[h] > dt) {
                        bestDwellTimes[h] = dt;
                    }
                }
            }
        }
        // Q: Why is incoming running times 1 shorter than departures?
        // A: Because when there is no arrivals array, the last departure is actually used for an
        // arrival.
        for (int h = 0; h < nHops; ++h) {
            bestRunningTimes[h] = Integer.MAX_VALUE;
            for (int t = 0; t < nTrips; ++t) {
                int rt = this.getRunningTime(h, t);
                if (bestRunningTimes[h] > rt) {
                    bestRunningTimes[h] = rt;
                }
            }
        }
        /* In large timetables, index stoptimes to allow binary searches over trips. */
        if (nTrips > INDEX_THRESHOLD) {
            LOG.trace("indexing pattern with {} trips", nTrips);
            index();
        } else {
            arrivalsIndex = null;
            departuresIndex = null;
        }
        /* Detect trip overlap modulo 24 hours. Allows departure search optimizations. */
        minDepart = Integer.MAX_VALUE;
        maxArrive = Integer.MIN_VALUE;
        for (int t = 0; t < nTrips; ++t) {
            int depart = getDepartureTime(0, t);
            int arrive = getArrivalTime(nHops - 1, t);
            if (minDepart > depart) {
                minDepart = depart;
            }
            if (maxArrive < arrive) {
                maxArrive = arrive;
            }
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
            if (tt.getTrip().getId().equals(tripId)) return ret;
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
     * Apply the TripUpdate to the appropriate TripTimes from this Timetable.
     * The existing TripTimes must not be modified directly because they may be shared with
     * the underlying scheduledTimetable, or other updated Timetables.
     * The StoptimeUpdater performs the protective copying of this Timetable. It is not done in
     * this update method to avoid repeatedly cloning the same Timetable when several updates
     * are applied to it at once.
     * @return whether or not the timetable actually changed as a result of this operation
     * (maybe it should do the cloning and return the new timetable to enforce copy-on-write?)
     */
    public boolean update(TripUpdate tripUpdate, String agencyId, TimeZone timeZone,
            ServiceDate updateServiceDate) {
        if (tripUpdate == null) {
            LOG.error("A null TripUpdate pointer was passed to the Timetable class update method.");
            return false;
        } else try {
             // Though all timetables have the same trip ordering, some may have extra trips due to
             // the dynamic addition of unscheduled trips.
             // However, we want to apply trip updates on top of *scheduled* times
            if (!tripUpdate.hasTrip()) {
                LOG.error("TripUpdate object has no TripDescriptor field.");
                return false;
            }

            TripDescriptor tripDescriptor = tripUpdate.getTrip();

            if (!tripDescriptor.hasTripId()) {
                LOG.error("TripDescriptor object has no TripId field");
                return false;
            }
            AgencyAndId tripId = new AgencyAndId(agencyId, tripDescriptor.getTripId());

            int tripIndex = getTripIndex(tripId);
            if (tripIndex == -1) {
                LOG.info("tripId {} not found in pattern.", tripId);
                return false;
            } else {
                LOG.trace("tripId {} found at index {} in scheduled timetable.", tripId, tripIndex);
            }

            TripTimes newTimes = new TripTimes(getTripTimes(tripIndex));

            if (tripDescriptor.hasScheduleRelationship() && tripDescriptor.getScheduleRelationship()
                    == TripDescriptor.ScheduleRelationship.CANCELED) {
                newTimes.cancel();
            } else {
                // The GTFS-RT reference specifies that StopTimeUpdates are sorted by stop_sequence.
                Iterator<StopTimeUpdate> updates = tripUpdate.getStopTimeUpdateList().iterator();
                if (!updates.hasNext()) {
                    LOG.warn("Won't apply zero-length trip update to trip {}.", tripId);
                    return false;
                }
                StopTimeUpdate update = updates.next();

                int numHops = newTimes.getNumHops();
                Integer delay = null;

                for (int i = 0; i <= numHops; i++) {
                    boolean match = false;
                    if (update != null) {
                        if (update.hasStopSequence()) {
                            match = update.getStopSequence() == newTimes.getStopSequence(i);
                        } else if (update.hasStopId()) {
                            match = pattern.getStop(i).getId().getId().equals(update.getStopId());
                        }
                    }

                    if (match) {
                        StopTimeUpdate.ScheduleRelationship scheduleRelationship =
                                update.hasScheduleRelationship() ? update.getScheduleRelationship()
                                : StopTimeUpdate.ScheduleRelationship.SCHEDULED;
                        if (scheduleRelationship == StopTimeUpdate.ScheduleRelationship.SKIPPED) {
                            // Not really supported right now
                            if (i > 0) newTimes.updateArrivalTime(i - 1, TripTimes.CANCELED);
                            if (i < numHops) newTimes.updateDepartureTime(i, TripTimes.CANCELED);
                        } else if (scheduleRelationship ==
                                StopTimeUpdate.ScheduleRelationship.NO_DATA) {
                            if (i > 0) newTimes.updateArrivalDelay(i - 1, 0);
                            if (i < numHops) newTimes.updateDepartureDelay(i, 0);
                            delay = 0;
                        } else {
                            long today = updateServiceDate.getAsDate(timeZone).getTime() / 1000;

                            if (i == 0) {
                                if (update.hasArrival()) {
                                    StopTimeEvent arrival = update.getArrival();
                                    if (arrival.hasDelay()) {
                                        delay = arrival.getDelay();
                                    }   // Arrival times aren't stored for the first stop - no else.
                                }
                            } else {
                                if (update.hasArrival()) {
                                    StopTimeEvent arrival = update.getArrival();
                                    if (arrival.hasDelay()) {
                                        delay = arrival.getDelay();
                                        newTimes.updateArrivalDelay(i - 1, delay);
                                    } else if (arrival.hasTime()) {
                                        newTimes.updateArrivalTime(i - 1,
                                                (int) (arrival.getTime() - today));
                                        delay = newTimes.getArrivalDelay(i - 1);
                                    } else {
                                        LOG.error("Arrival time at index {} is erroneous.", i);
                                        return false;
                                    }
                                } else {
                                    if (delay == null) {
                                        newTimes.updateArrivalTime(i - 1, TripTimes.PASSED);
                                    } else {
                                        newTimes.updateArrivalDelay(i - 1, delay);
                                    }
                                }
                            }

                            if (i < numHops) {
                                if (update.hasDeparture()) {
                                    StopTimeEvent departure = update.getDeparture();
                                    if (departure.hasDelay()) {
                                        delay = departure.getDelay();
                                        newTimes.updateDepartureDelay(i, delay);
                                    } else if (departure.hasTime()) {
                                        newTimes.updateDepartureTime(i,
                                                (int) (departure.getTime() - today));
                                        delay = newTimes.getDepartureDelay(i);
                                    } else {
                                        LOG.error("Departure time at index {} is erroneous.", i);
                                        return false;
                                    }
                                } else {
                                    if (delay == null) {
                                        newTimes.updateDepartureTime(i, TripTimes.PASSED);
                                    } else {
                                        newTimes.updateDepartureDelay(i, delay);
                                    }
                                }
                            }
                        }

                        if (updates.hasNext()) {
                            update = updates.next();
                        } else {
                            update = null;
                        }
                    } else {
                        if (delay == null) {
                            if (i > 0) newTimes.updateArrivalTime(i - 1, TripTimes.PASSED);
                            if (i < numHops) newTimes.updateDepartureTime(i, TripTimes.PASSED);
                        } else {
                            if (i > 0) newTimes.updateArrivalDelay(i - 1, delay);
                            if (i < numHops) newTimes.updateDepartureDelay(i, delay);
                        }
                    }
                }

                newTimes.compactArrivalsAndDepartures();
                if (update != null) {
                    LOG.error("Part of a TripUpdate object could not be applied successfully.");
                    return false;
                }
            }
            if (!newTimes.timesIncreasing()) {
                LOG.error("TripTimes are non-increasing after applying GTFS-RT delay propagation.");
                return false;
            }

            // Update succeeded, save the new TripTimes back into this Timetable.
            tripTimes.set(tripIndex, newTimes);
        } catch (Exception e) { // prevent server from dying while debugging
            e.printStackTrace();
            return false;
        }

        LOG.trace("A valid TripUpdate object was applied using the Timetable class update method.");
        return true;
    }

    /**
     * Add a trip to this Timetable. The Timetable must be analyzed, compacted, and indexed
     * any time trips are added, but this is not done automatically because it is time consuming
     * and should only be done once after an entire batch of trips are added.
     */
    public void addTrip(Trip trip, List<StopTime> stopTimes) {
        TripTimes tripTime = new TripTimes(trip, stopTimes);
        if(!tripTimes.isEmpty()) {
            TripTimes firstTripTime = tripTimes.get(0);
            tripTime.compactStopSequence(firstTripTime);
        }
        tripTimes.add(tripTime);
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

    public boolean isValidFor(ServiceDate serviceDate) {
        return this.serviceDate == null || this.serviceDate.equals(serviceDate);
    }

    /**
     * @return true if any two trips in this timetable overlap, modulo 24 hours. Helps determine
     * whether we need to look at more than one day when performing departure/arrival searches.
     */
    private boolean tripsOverlap() {
        return maxArrive - minDepart > (24 * 60 * 60);
    }

    /** @return true if any trip in this timetable contains a stoptime greater than 24 hours. */
    private boolean crossesMidnight() {
        return maxArrive > (24 * 60 * 60);
    }
}
