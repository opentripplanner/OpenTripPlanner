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
public class TimetableResolver {

    protected static class SortedTimetableComparator implements Comparator<Timetable> {
        @Override
        public int compare(Timetable t1, Timetable t2) {
            return t1.serviceDate.compareTo(t2.serviceDate);
        }
    }
    
    /**
     * Class to use as key in HashMap containing trip id and service date
     */
    protected class TripIdAndServiceDate {
        private final String tripId;
        private final ServiceDate serviceDate;
        
        public TripIdAndServiceDate(final String tripId, final ServiceDate serviceDate) {
            this.tripId = tripId;
            this.serviceDate = serviceDate;
        }
        
        public String getTripId() {
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

    private static final Logger LOG = LoggerFactory.getLogger(TimetableResolver.class);

    // Use HashMap not Map so we can clone.
    // if this turns out to be slow/spacious we can use an array with integer pattern indexes
    // The SortedSet members are copy-on-write
    // FIXME: this could be made into a flat hashtable with compound keys.
    private HashMap<TripPattern, SortedSet<Timetable>> timetables =
            new HashMap<TripPattern, SortedSet<Timetable>>();
    
    /**
     * <p>
     * Map containing the last <b>added</b> trip pattern given a trip id (without agency) and a
     * service date as a result of a call to {@link #update(TripPattern, TripTimes, ServiceDate)}
     * with trip times of a trip that didn't exist yet in the trip pattern.
     * </p>
     * <p>
     * This is a HashMap and not a Map so the clone function is available.
     * </p>
     */
    private HashMap<TripIdAndServiceDate, TripPattern> lastAddedTripPattern = new HashMap<>();

    /** A set of all timetables which have been modified and are waiting to be indexed. */
    private Set<Timetable> dirty = new HashSet<Timetable>();

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
     * a result of a call to {@link #update(TripPattern, TripTimes, ServiceDate)} with trip times of
     * a trip that didn't exist yet in the trip pattern.
     * 
     * @param tripId trip id (without agency)
     * @param serviceDate service date
     * @return last added trip pattern; null if trip never was added to a trip pattern
     */
    public TripPattern getLastAddedTripPattern(String tripId, ServiceDate serviceDate) {
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
        
        // synchronization prevents commits/snapshots while update is in progress
        synchronized(this) {
            if (dirty == null)
                throw new ConcurrentModificationException("This TimetableResolver is read-only.");
            Timetable tt = resolve(pattern, serviceDate);
            // we need to perform the copy of Timetable here rather than in Timetable.update()
            // to avoid repeatedly copying in case several updates are applied to the same timetable
            if ( ! dirty.contains(tt)) {
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
                dirty.add(tt);
            }
            
            // Assume all trips in a pattern are from the same feed, which should be the case.
            // Find trip index
            int tripIndex = tt.getTripIndex(updatedTripTimes.trip.getId());
            if (tripIndex == -1) {
                // Trip not found, add it
                tt.addTripTimes(updatedTripTimes);
                // Remember this pattern for the added trip id and service date
                String tripId = updatedTripTimes.trip.getId().getId();
                TripIdAndServiceDate tripIdAndServiceDate = new TripIdAndServiceDate(tripId, serviceDate);
                lastAddedTripPattern.put(tripIdAndServiceDate, pattern);
            } else {
                // Set updated trip times of trip
                tt.setTripTimes(tripIndex, updatedTripTimes);
            }
            
            // The time tables are finished during the commit
            
            return true;
        }
    }

    /**
     * This produces a small delay of typically around 50ms, which is almost entirely due to
     * the indexing step. Cloning the map is much faster (2ms).
     * It is perhaps better to index timetables as they are changed to avoid experiencing all
     * this lag at once, but we want to avoid re-indexing when receiving multiple updates for
     * the same timetable in rapid succession. This compromise is expressed by the
     * maxSnapshotFrequency property of StoptimeUpdater. The indexing could be made much more
     * efficient as well.
     * @return an immutable copy of this TimetableResolver with all updates applied
     */
    public TimetableResolver commit() {
        return commit(false);
    }

    @SuppressWarnings("unchecked")
    public TimetableResolver commit(boolean force) {
        TimetableResolver ret = new TimetableResolver();
        // synchronization prevents updates while commit/snapshot in progress
        synchronized(this) {
            if (dirty == null) {
                throw new ConcurrentModificationException("This TimetableResolver is read-only.");
            }
            if (!force && !this.isDirty()) return null;
            for (Timetable tt : dirty) {
                tt.finish(); // summarize, index, etc. the new timetables
            }
            ret.timetables = (HashMap<TripPattern, SortedSet<Timetable>>) this.timetables.clone();
            ret.lastAddedTripPattern = (HashMap<TripIdAndServiceDate, TripPattern>)
                    this.lastAddedTripPattern.clone();
            this.dirty.clear();
        }
        ret.dirty = null; // mark the snapshot as henceforth immutable
        return ret;
    }

    /**
     * Removes all Timetables which are valid for a ServiceDate on-or-before the one supplied.
     */
    public boolean purgeExpiredData(ServiceDate serviceDate) {
        synchronized(this) {
            if (dirty == null) {
                throw new ConcurrentModificationException("This TimetableResolver is read-only.");
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
    }

    public boolean isDirty() {
        if (dirty == null) return false;
        return dirty.size() > 0;
    }

    public String toString() {
        String d = dirty == null ? "committed" : String.format("%d dirty", dirty.size());
        return String.format("Timetable snapshot: %d timetables (%s)", timetables.size(), d);
    }
}
