package org.opentripplanner.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

// this is only currently in edgetype because that's where Trippattern is.
// move these classes elsewhere.

/**
 * Part of concurrency control for stoptime updates.
 *
 * All updates should be performed on a snapshot before it is handed off to any searches.
 * A single snapshot should be used for an entire search, and should remain unchanged
 * for that duration to provide a consistent view not only of trips that have been boarded, but of
 * relative arrival and departure times of other trips that have not necessarily been boarded.
 *
 * At this point, only one writing thread at a time is supported.
 *
 *  TODO OTP2 - Move this to package: org.opentripplanner.model
 *            - after ass Entur NeTEx PRs are merged.
 */
public class TimetableSnapshot {

    protected static class SortedTimetableComparator implements Comparator<Timetable> {
        @Override
        public int compare(Timetable t1, Timetable t2) {
            return t1.serviceDate.compareTo(t2.serviceDate);
        }
    }
    
    /**
     * Class to use as key in HashMap containing feed id, trip id and service date
     * TODO shouldn't this be a static class?
     */
    protected class TripIdAndServiceDate {
        private final FeedScopedId tripId;
        private final ServiceDate serviceDate;
        
        public TripIdAndServiceDate(final FeedScopedId tripId, final ServiceDate serviceDate) {
            this.tripId = tripId;
            this.serviceDate = serviceDate;
        }

        public FeedScopedId getTripId() {
            return tripId;
        }

        public ServiceDate getServiceDate() {
            return serviceDate;
        }


        @Override
        public int hashCode() {
            int result = Objects.hash(tripId, serviceDate);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TripIdAndServiceDate other = (TripIdAndServiceDate) obj;
            boolean result = Objects.equals(this.tripId, other.tripId) &&
                    Objects.equals(this.serviceDate, other.serviceDate);
            return result;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshot.class);
    
    /**
     * The timetables for different days, for each TripPattern (each sequence of stops on a particular Route) for which
     * we have an updated Timetable. The keys include both TripPatterns from the scheduled GTFS, and TripPatterns added
     * by realtime messages and tracked by the TripPatternCache. Note that the keys will not include all scheduled
     * TripPatterns, only those for which we've got an update.
     * We use a HashMap rather than a Map so we can clone it. If this turns out to be slow/spacious we can use an array
     * with integer pattern indexes. The SortedSet members are copy-on-write.
     * FIXME: this could be made into a flat hashtable with compound keys.
     */
    private HashMap<TripPattern, SortedSet<Timetable>> timetables = new HashMap();

    /**
     * <p>
     * Map containing the last <b>added</b> trip pattern given a trip id (without agency) and a
     * service date as a result of a call to {@link #update(TripPattern, TripTimes, ServiceDate)}
     * with trip times of a trip that didn't exist yet in the trip pattern.
     * </p>
     * <p>
     * This is a HashMap and not a Map so the clone function is available.
     * </p>
     * TODO clarify what it means to say "last" added trip pattern. There can be more than one? What happens to the older ones?
     */
    private HashMap<TripIdAndServiceDate, TripPattern> lastAddedTripPattern = new HashMap<>();

    /**
     * This maps contains all of the new or updated TripPatterns added by realtime data indexed on
     * stop. This has to be kept in order for them to be included in the stop times api call on a
     * specific stop.
     *
     * TODO Find a generic way to keep all realtime indexes.
     */
    private Multimap<Stop, TripPattern> patternsForStop = ArrayListMultimap.create();
    
    /**
     * Boolean value indicating that timetable snapshot is read only if true. Once it is true, it shouldn't
     * be possible to change it to false anymore.
     */
    private boolean readOnly = false;

    /**
     * Boolean value indicating that this timetable snapshot contains changes compared to the state
     * of the last commit if true.
     */
    private boolean dirty = false;
    
    /**
     * A set of all timetables which have been modified and are waiting to be indexed. When
     * <code>dirty</code> is <code>null</code>, it indicates that the snapshot is read-only.
     */
    private Set<Timetable> dirtyTimetables = new HashSet<>();

    /**
     * Returns an updated timetable for the specified pattern if one is available in this snapshot,
     * or the originally scheduled timetable if there are no updates in this snapshot.
     */
    public Timetable resolve(TripPattern pattern, ServiceDate serviceDate) {
        SortedSet<Timetable> sortedTimetables = timetables.get(pattern);

        if(sortedTimetables != null && serviceDate != null) {
            for(Timetable timetable : sortedTimetables) {
                if (timetable != null && timetable.isValidFor(serviceDate)) {
                    return timetable;
                }
            }
        }

        return pattern.scheduledTimetable;
    }
    
    /**
     * Get the last <b>added</b> trip pattern given a trip id (without agency) and a service date as
     * a result of a call to {@link #update(TripPattern, TripTimes, ServiceDate)} with trip times of
     * a trip that didn't exist yet in the trip pattern.
     * TODO clarify what it means to say "last" added trip pattern. There can be more than one? What happens to the older ones?
     *
     * @param tripId trip id
     * @param serviceDate service date
     * @return last added trip pattern; null if trip never was added to a trip pattern
     */
    public TripPattern getLastAddedTripPattern(FeedScopedId tripId, ServiceDate serviceDate) {
        TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(tripId, serviceDate);
        TripPattern pattern = lastAddedTripPattern.get(tripIdAndServiceDate);
        return pattern;
    }

    /**
     * Update the trip times of one trip in a timetable of a trip pattern. If the trip of the trip
     * times does not exist yet in the timetable, add it.
     * 
     * @param pattern trip pattern
     * @param updatedTripTimes updated trip times
     * @param serviceDate service day for which this update is valid
     * @return whether or not the update was actually applied
     */
    public boolean update(TripPattern pattern, TripTimes updatedTripTimes, ServiceDate serviceDate) {
        // Preconditions
        Preconditions.checkNotNull(pattern);
        Preconditions.checkNotNull(serviceDate);
        
        if (readOnly) {
            throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
        }
        
        Timetable tt = resolve(pattern, serviceDate);
        // we need to perform the copy of Timetable here rather than in Timetable.update()
        // to avoid repeatedly copying in case several updates are applied to the same timetable
        if ( ! dirtyTimetables.contains(tt)) {
            Timetable old = tt;
            tt = new Timetable(tt, serviceDate);
            SortedSet<Timetable> sortedTimetables = timetables.get(pattern);
            if(sortedTimetables == null) {
                sortedTimetables = new TreeSet<>(new SortedTimetableComparator());
            } else {
                SortedSet<Timetable> temp =
                    new TreeSet<>(new SortedTimetableComparator());
                temp.addAll(sortedTimetables);
                sortedTimetables = temp;
            }
            if(old.serviceDate != null)
                sortedTimetables.remove(old);
            sortedTimetables.add(tt);
            timetables.put(pattern, sortedTimetables);
            dirtyTimetables.add(tt);
            dirty = true;
        }
        
        // Assume all trips in a pattern are from the same feed, which should be the case.
        // Find trip index
        int tripIndex = tt.getTripIndex(updatedTripTimes.trip.getId());
        if (tripIndex == -1) {
            // Trip not found, add it
            tt.addTripTimes(updatedTripTimes);
            // Remember this pattern for the added trip id and service date
            FeedScopedId tripId = updatedTripTimes.trip.getId();
            TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(tripId, serviceDate);
            lastAddedTripPattern.put(tripIdAndServiceDate, pattern);
        } else {
            // Set updated trip times of trip
            tt.setTripTimes(tripIndex, updatedTripTimes);
        }

        // To make these trip patterns visible for departureRow searches.
        addPatternToIndex(pattern);
        
        // The time tables are finished during the commit
        
        return true;
    }

    /**
     * This produces a small delay of typically around 50ms, which is almost entirely due to
     * the indexing step. Cloning the map is much faster (2ms).
     * It is perhaps better to index timetables as they are changed to avoid experiencing all
     * this lag at once, but we want to avoid re-indexing when receiving multiple updates for
     * the same timetable in rapid succession. This compromise is expressed by the
     * maxSnapshotFrequency property of StoptimeUpdater. The indexing could be made much more
     * efficient as well.
     * @return an immutable copy of this TimetableSnapshot with all updates applied
     */
    public TimetableSnapshot commit() {
        return commit(null, false);
    }

    @SuppressWarnings("unchecked")
    public TimetableSnapshot commit(TransitLayerUpdater transitLayerUpdater, boolean force) {
        if (readOnly) {
            throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
        }
        
        TimetableSnapshot ret = new TimetableSnapshot();
        if (!force && !this.isDirty()) return null;
        for (Timetable tt : dirtyTimetables) {
            tt.finish(); // summarize, index, etc. the new timetables
        }
        ret.timetables = (HashMap<TripPattern, SortedSet<Timetable>>) this.timetables.clone();
        ret.lastAddedTripPattern = (HashMap<TripIdAndServiceDate, TripPattern>)
                this.lastAddedTripPattern.clone();

        if (transitLayerUpdater != null) {
            transitLayerUpdater.update(dirtyTimetables);
        }

        this.dirtyTimetables.clear();
        this.dirty = false;

        ret.setPatternsForStop(HashMultimap.create(this.patternsForStop));

        ret.readOnly = true; // mark the snapshot as henceforth immutable
        return ret;
    }

    /**
     * Clear all data of snapshot for the provided feed id
     *
     * @param feedId feed id to clear the snapshop for
     */
    public void clear(String feedId) {
        if (readOnly) {
            throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
        }
        // Clear all data from snapshot.
        boolean timetableWasModified = clearTimetable(feedId);
        boolean lastAddedWasModified = clearLastAddedTripPattern(feedId);

        // If this snapshot was modified, it will be dirty after the clear actions.
        if (timetableWasModified || lastAddedWasModified) {
            dirty = true;
        }
    }

    /**
     * Clear timetable for all patterns matching the provided feed id.
     *
     * @param feedId feed id to clear out
     * @return true if the timetable changed as a result of the call
     */
    protected boolean clearTimetable(String feedId) {
        return timetables.keySet().removeIf(tripPattern -> feedId.equals(tripPattern.getFeedId()));
    }

    /**
     * Clear all last added trip patterns matching the provided feed id.
     *
     * @param feedId feed id to clear out
     * @return true if the lastAddedTripPattern changed as a result of the call
     */
    protected boolean clearLastAddedTripPattern(String feedId) {
        return lastAddedTripPattern.keySet().removeIf(
            lastAddedTripPattern ->
                feedId.equals(lastAddedTripPattern.getTripId().getFeedId())
        );
    }

    /**
     * Removes all Timetables which are valid for a ServiceDate on-or-before the one supplied.
     */
    public boolean purgeExpiredData(ServiceDate serviceDate) {
        if (readOnly) {
            throw new ConcurrentModificationException("This TimetableSnapshot is read-only.");
        }

        boolean modified = false;
        for (Iterator<TripPattern> it = timetables.keySet().iterator(); it.hasNext();){
            TripPattern pattern = it.next();
            SortedSet<Timetable> sortedTimetables = timetables.get(pattern);
            SortedSet<Timetable> toKeepTimetables =
                    new TreeSet<Timetable>(new SortedTimetableComparator());
            for(Timetable timetable : sortedTimetables) {
                if(serviceDate.compareTo(timetable.serviceDate) < 0) {
                    toKeepTimetables.add(timetable);
                } else {
                    modified = true;
                }
            }

            if(toKeepTimetables.isEmpty()) {
                it.remove();
            } else {
                timetables.put(pattern, toKeepTimetables);
            }
        }
        
        // Also remove last added trip pattern for days that are purged
        for (Iterator<Entry<TripIdAndServiceDate, TripPattern>> iterator = lastAddedTripPattern
                .entrySet().iterator(); iterator.hasNext();) {
            TripIdAndServiceDate tripIdAndServiceDate = iterator.next().getKey();
            if (serviceDate.compareTo(tripIdAndServiceDate.getServiceDate()) >= 0) {
                iterator.remove();
                modified = true;
            }
        }

        return modified;
    }

    public boolean isDirty() {
        if (readOnly) return false;
        return dirty;
    }

    public String toString() {
        String d = readOnly ? "committed" : String.format("%d dirty", dirtyTimetables.size());
        return String.format("Timetable snapshot: %d timetables (%s)", timetables.size(), d);
    }

    /**
     * @return all TripPatterns for which we have any updated timetables created by realtime messages, including both
     *         patterns that were in the scheduled (static) transit data and those that were added to this snapshot by
     *         rerouted or added trips.
     */
    public Collection<TripPattern> getAllRealtimeTripPatterns () {
        return timetables.keySet();
    }

    private void addPatternToIndex(TripPattern tripPattern) {
        for (Stop stop: tripPattern.getStops()) {
            patternsForStop.put(stop, tripPattern);
        }
    }

    public Collection<TripPattern> getPatternsForStop(Stop stop) {
        return patternsForStop.get(stop);
    }

    public void setPatternsForStop(Multimap<Stop, TripPattern> patternsForStop) {
        this.patternsForStop = patternsForStop;
    }
}
