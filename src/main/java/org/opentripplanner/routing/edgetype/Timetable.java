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

import static org.opentripplanner.routing.trippattern.TripTimes.formatSeconds;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.beust.jcommander.internal.Lists;
import lombok.Getter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

import javax.ws.rs.GET;


/**
 * Timetables provide most of the TripPattern functionality. Each TripPattern may possess more than
 * one Timetable when stop time updates are being applied: one for the scheduled stop times, one for
 * each snapshot of updated stop times, another for a working buffer of updated stop times, etc.
 */
public class Timetable implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Timetable.class);
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    /**
     * A circular reference between TripPatterns and their scheduled (non-updated) timetables.
     */
    @Getter
    private final TripPattern pattern;

    /**
     * Contains one TripTimes object for each scheduled trip (even cancelled ones) and possibly
     * additional TripTimes objects for unscheduled trips. Frequency entries are stored separately.
     */
    @Getter
    private final List<TripTimes> tripTimes = Lists.newArrayList();

    /**
     * Contains one FrequencyEntry object for each block of frequency-based trips.
     */
    @Getter
    private final List<FrequencyEntry> frequencyEntries = Lists.newArrayList();

    /**
     * The ServiceDate for which this (updated) timetable is valid.
     * If null, then it is valid for all dates.
     */
    @Getter
    private final ServiceDate serviceDate;

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
    public Timetable(TripPattern pattern) {
        this.pattern = pattern;
        this.serviceDate = null;
    }

    /**
     * Copy constructor: create an un-indexed Timetable with the same TripTimes as the specified timetable.
     */
    Timetable (Timetable tt, ServiceDate serviceDate) {
        tripTimes.addAll(tt.tripTimes);
        this.serviceDate = serviceDate;
        this.pattern = tt.pattern;
    }

    /**
     * Get the next (previous) trip that departs (arrives) from the specified stop at or after
     * (before) the specified time.
     * @return the TripTimes object representing the (possibly updated) best trip, or null if no
     * trip matches both the time and other criteria.
     */
    protected TripTimes getNextTrip(State s0, ServiceDay serviceDay, int stopIndex, boolean boarding) {
        /* Search at the state's time, but relative to midnight on the given service day. */
        int time = serviceDay.secondsSinceMidnight(s0.getTimeSeconds());
        // Skip this search if it's impossible to use any trip.
        if (boarding ? (time > this.maxArrive) : (time < this.minDepart)) return null;
        // NOTE the time is sometimes negative here. That is fine, we search for the first trip of the day.
        /* Establish whether we have a rented _or_ owned bicycle. */
        boolean haveBicycle = s0.getNonTransitMode() == TraverseMode.BICYCLE; 
        TripTimes bestTrip = null;
        int index;
        Stop currentStop = pattern.getStop(stopIndex);
        // Linear search through the timetable looking for the best departure.
        // We no longer use a binary search on Timetables because:
        // 1. we allow combining trips from different service IDs on the same tripPattern.
        // 2. We mix frequency-based and one-off TripTimes together on tripPatterns.
        // 3. Stoptimes may change with realtime updates, and we cannot count on them being sorted.
        //    The complexity of keeping sorted indexes up to date does not appear to be worth the
        //    apparently minor speed improvement.
        int bestTime = boarding ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        // Hoping JVM JIT will distribute the loop over the if clauses as needed.
        // We could invert this and skip some service days based on schedule overlap as in RRRR.
        for (TripTimes tt : tripTimes) {
            if ( ! serviceDay.serviceRunning(tt.serviceCode)) continue;
            if (boarding) {
                int depTime = tt.getDepartureTime(stopIndex);
                if (depTime < 0) continue;
                if (depTime >= time && depTime < bestTime && tt.tripAcceptable(s0,
                        currentStop, serviceDay, haveBicycle, stopIndex, boarding)) {
                    bestTrip = tt;
                    bestTime = depTime;
                }
            } else {
                int arvTime = tt.getArrivalTime(stopIndex);
                if (arvTime < 0) continue;
                if (arvTime <= time && arvTime > bestTime && tt.tripAcceptable(s0,
                        currentStop, serviceDay, haveBicycle, stopIndex, boarding)) {
                    bestTrip = tt;
                    bestTime = arvTime;
                }
            }
        }
        // ACK all logic is identical to above.
        // A sign that FrequencyEntries and TripTimes need a common interface.
        FrequencyEntry bestFreq = null;
        for (FrequencyEntry freq : frequencyEntries) {
            TripTimes tt = freq.tripTimes;
            if ( ! serviceDay.serviceRunning(tt.serviceCode)) continue;
            LOG.debug("  running freq {}", freq);
            if (boarding) {
                int depTime = freq.nextDepartureTime(stopIndex, time);
                if (depTime < 0) continue;
                if (depTime >= time && depTime < bestTime && tt.tripAcceptable(s0,
                        currentStop, serviceDay, haveBicycle, stopIndex, boarding)) {
                    bestFreq = freq;
                    bestTime = depTime;
                }
            } else {
                int arvTime = freq.prevArrivalTime(stopIndex, time);
                if (arvTime < 0) continue;
                if (arvTime <= time && arvTime > bestTime && tt.tripAcceptable(s0,
                        currentStop, serviceDay, haveBicycle, stopIndex, boarding)) {
                    bestFreq = freq;
                    bestTime = arvTime;
                }
            }
        }
        if (bestFreq != null) {
            // A FrequencyEntry beat all the TripTimes.
            // Materialize that FrequencyEntry entry at the given time.
            bestTrip = bestFreq.tripTimes.timeShift(stopIndex, bestTime, boarding);
        }
        return bestTrip;
    }

    /**
     * Finish off a Timetable once all TripTimes have been added to it. This involves caching
     * lower bounds on the running times and dwell times at each stop, and may perform other
     * actions to compact the data structure such as trimming and deduplicating arrays.
     */
    public void finish() {
        int nStops = pattern.stopPattern.size;
        int nHops = nStops - 1;
        /* Find lower bounds on dwell and running times at each stop. */
        bestDwellTimes = new int[nHops];
        bestRunningTimes = new int[nHops];
        Arrays.fill(bestDwellTimes, Integer.MAX_VALUE);
        Arrays.fill(bestRunningTimes, Integer.MAX_VALUE);
        for (TripTimes tt : tripTimes) {
            for (int h = 0; h < nHops; ++h) {
                int dt = tt.getDwellTime(h);
                if (bestDwellTimes[h] > dt) {
                    bestDwellTimes[h] = dt;
                }
                int rt = tt.getRunningTime(h);
                if (bestRunningTimes[h] > rt) {
                    bestRunningTimes[h] = rt;
                }
            }
        }
        /* Detect trip overlap modulo 24 hours. Allows departure search optimizations. */
        minDepart = Integer.MAX_VALUE;
        maxArrive = Integer.MIN_VALUE;
        for (TripTimes tt : tripTimes) {
            int depart = tt.getDepartureTime(0);
            if (minDepart > depart) {
                minDepart = depart;
            }
            int arrive = tt.getArrivalTime(nStops - 1);
            if (maxArrive < arrive) {
                maxArrive = arrive;
            }
        }
    }

    public class ArrivalsIterator implements Iterator<Integer> {

        int nextPosition = 0;

        private int stopIndex;

        public ArrivalsIterator(int stopIndex) {
            this.stopIndex = stopIndex;
        }

        @Override
        public boolean hasNext() {
            return nextPosition < tripTimes.size();
        }

        @Override
        public Integer next() {
            return tripTimes.get(nextPosition++).getArrivalTime(stopIndex - 1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /** Gets all the arrival times at a given stop (not used in routing) */
    public Iterator<Integer> getArrivalTimes(int stopIndex) {
        return new ArrivalsIterator(stopIndex);
    }

    /** @return the index of TripTimes for this Trip(Id) in this particular Timetable */
    public int getTripIndex(AgencyAndId tripId) {
        int ret = 0;
        for (TripTimes tt : tripTimes) {
            // could replace linear search with indexing in stoptime updater, but not necessary
            // at this point since the updater thread is far from pegged.
            if (tt.trip.getId().equals(tripId)) return ret;
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
        // FIXME this method is totally wrong, it uses hops not stops
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

                int numStops = newTimes.getNumStops();
                Integer delay = null;

                for (int i = 0; i < numStops; i++) {
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
                            // TODO: Handle partial trip cancellations
                            LOG.warn("Partially canceled trips are currently unsupported." +
                                    " Skipping TripUpdate.");
                            return false;
                        } else if (scheduleRelationship ==
                                StopTimeUpdate.ScheduleRelationship.NO_DATA) {
                            if (i > 0) newTimes.updateArrivalDelay(i - 1, 0);
                            if (i < numStops - 1) newTimes.updateDepartureDelay(i, 0);
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
                                        if (arrival.hasTime()) {
                                            newTimes.updateArrivalTime(i - 1,
                                                    (int) (arrival.getTime() - today));
                                        } else {
                                            newTimes.updateArrivalDelay(i - 1, delay);
                                        }
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
                                        newTimes.updateArrivalTime(i - 1, TripTimes.UNAVAILABLE);
                                    } else {
                                        newTimes.updateArrivalDelay(i - 1, delay);
                                    }
                                }
                            }

                            if (i < numStops - 1) {
                                if (update.hasDeparture()) {
                                    StopTimeEvent departure = update.getDeparture();
                                    if (departure.hasDelay()) {
                                        delay = departure.getDelay();
                                        if (departure.hasTime()) {
                                            newTimes.updateDepartureTime(i,
                                                    (int) (departure.getTime() - today));
                                        } else {
                                            newTimes.updateDepartureDelay(i, delay);
                                        }
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
                                        newTimes.updateDepartureTime(i, TripTimes.UNAVAILABLE);
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
                            if (i > 0) newTimes.updateArrivalTime(i - 1, TripTimes.UNAVAILABLE);
                            if (i < numStops - 1) newTimes.updateDepartureTime(i, TripTimes.UNAVAILABLE);
                        } else {
                            if (i > 0) newTimes.updateArrivalDelay(i - 1, delay);
                            if (i < numStops - 1) newTimes.updateDepartureDelay(i, delay);
                        }
                    }
                }
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
     * Note that the trip is not added to the enclosing pattern here, but in the pattern's wrapper function.
     * Here we don't know if it's a scheduled trip or a realtime-added trip.
     */
    public void addTripTimes(TripTimes tt) {
        tripTimes.add(tt);
    }

    /**
     * Add a frequency entry to this Timetable. See addTripTimes method. Maybe Frequency Entries should
     * just be TripTimes for simplicity.
     */
    public void addFrequencyEntry(FrequencyEntry freq) {
        frequencyEntries.add(freq);
    }

    /**
     * Check that all dwell times at the given stop are zero, which allows removing the dwell edge.
     * TODO we should probably just eliminate dwell-deletion. It won't be important if we get rid of transit edges.
     */
    boolean allDwellsZero(int hopIndex) {
        for (TripTimes tt : tripTimes) {
            if (tt.getDwellTime(hopIndex) != 0) {
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

    /** Find and cache service codes. Duplicates information in trip.getServiceId for optimization. */
    // TODO maybe put this is a more appropriate place
    public void setServiceCodes (Map<AgencyAndId, Integer> serviceCodes) {
        for (TripTimes tt : this.tripTimes) {
            tt.serviceCode = serviceCodes.get(tt.trip.getServiceId());
        }
        // Repeated code... bad sign...
        for (FrequencyEntry freq : this.frequencyEntries) {
            TripTimes tt = freq.tripTimes;
            tt.serviceCode = serviceCodes.get(tt.trip.getServiceId());
        }
    }

} 
