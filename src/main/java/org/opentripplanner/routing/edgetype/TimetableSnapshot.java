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

import java.util.*;
import java.util.Map.Entry;

import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

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
     */
    protected class TripIdAndServiceDate {
        private final String feedId;
        private final String tripId;
        private final ServiceDate serviceDate;
        
        public TripIdAndServiceDate(final String feedId, final String tripId, final ServiceDate serviceDate) {
            this.feedId = feedId;
            this.tripId = tripId;
            this.serviceDate = serviceDate;
        }

        public String getFeedId() {
            return feedId;
        }

        public String getTripId() {
            return tripId;
        }

        public ServiceDate getServiceDate() {
            return serviceDate;
        }


        @Override
        public int hashCode() {
            int result = Objects.hash(tripId, serviceDate, feedId);
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
                    Objects.equals(this.serviceDate, other.serviceDate) &&
                    Objects.equals(this.feedId, other.feedId);
            return result;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshot.class);
    
    // Use HashMap not Map so we can clone.
    // if this turns out to be slow/spacious we can use an array with integer pattern indexes
    // The SortedSet members are copy-on-write
    // FIXME: this could be made into a flat hashtable with compound keys.
    private HashMap<TripPattern, SortedSet<Timetable>> timetables =
            new HashMap<TripPattern, SortedSet<Timetable>>();

    /**
     * <p>
     * Map containing the last <b>added</b> trip pattern given a trip id (without agency) and a
     * service date as a result of a call to {@link #update(String feedId, TripPattern, TripTimes, ServiceDate)}
     * with trip times of a trip that didn't exist yet in the trip pattern.
     * </p>
     * <p>
     * This is a HashMap and not a Map so the clone function is available.
     * </p>
     */
    private HashMap<TripIdAndServiceDate, TripPattern> lastAddedTripPattern = new HashMap<>();
    
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
    private Set<Timetable> dirtyTimetables = new HashSet<Timetable>();

    /**
     * Returns an updated timetable for the specified pattern if one is available in this snapshot,
     * or the originally scheduled timetable if there are no updates in this snapshot.
     */
    public Timetable resolve(TripPattern pattern, ServiceDate serviceDate) {
        SortedSet<Timetable> sortedTimetables = timetables.get(pattern);

        if(sortedTimetables != null && serviceDate != null) {
            for(Timetable timetable : sortedTimetables) {
                if (timetable != null && timetable.isValidFor(serviceDate)) {
                    LOG.trace("returning modified timetable");
                    return timetable;
                }
            }
        }

        return pattern.scheduledTimetable;
    }
    
    /**
     * Get the last <b>added</b> trip pattern given a trip id (without agency) and a service date as
     * a result of a call to {@link #update(String feedId, TripPattern, TripTimes, ServiceDate)} with trip times of
     * a trip that didn't exist yet in the trip pattern.
     *
     * @param feedId feed id the trip id belongs to
     * @param tripId trip id (without agency)
     * @param serviceDate service date
     * @return last added trip pattern; null if trip never was added to a trip pattern
     */
    public TripPattern getLastAddedTripPattern(String feedId, String tripId, ServiceDate serviceDate) {
        TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(feedId, tripId, serviceDate);
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
    public boolean update(String feedId, TripPattern pattern, TripTimes updatedTripTimes, ServiceDate serviceDate) {
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
                sortedTimetables = new TreeSet<Timetable>(new SortedTimetableComparator());
            } else {
                SortedSet<Timetable> temp =
                        new TreeSet<Timetable>(new SortedTimetableComparator());
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
            String tripId = updatedTripTimes.trip.getId().getId();
            TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(feedId, tripId, serviceDate);
            lastAddedTripPattern.put(tripIdAndServiceDate, pattern);
        } else {
            // Set updated trip times of trip
            tt.setTripTimes(tripIndex, updatedTripTimes);
        }
        
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
        return commit(false);
    }

    @SuppressWarnings("unchecked")
    public TimetableSnapshot commit(boolean force) {
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
        this.dirtyTimetables.clear();
        this.dirty = false;

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
        return lastAddedTripPattern.keySet().removeIf(lastAddedTripPattern -> feedId.equals(lastAddedTripPattern.getFeedId()));
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
}
