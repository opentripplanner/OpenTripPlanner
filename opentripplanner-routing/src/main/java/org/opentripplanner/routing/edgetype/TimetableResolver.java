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

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.routing.trippattern.UpdateBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this is only currently in edgetype because that's where Trippattern is. move these classes elsewhere.

/**
 * Part of concurrency control for stoptime updates.
 * 
 * All updates should be performed on a snapshot before it is handed off to any searches.
 * A single snapshot should be used for an entire search, and should remain unchanged
 * for that duration to provide a consistent view not only of trips that have been boarded, 
 * but of relative arrival and departure times of other trips that have not necessarily been boarded.
 * 
 * At this point, only one writing thread at a time is supported.
 */
public class TimetableResolver {
    
    private static final Logger LOG = LoggerFactory.getLogger(TimetableResolver.class);

    // Use HashMap not Map so we can clone.
    // if this turns out to be slow/spacious we can use an array with integer pattern indexes
    private HashMap<TableTripPattern, Timetable> timetables = new HashMap<TableTripPattern, Timetable>(); 
    
    /** A set of all timetables which have been modified and are waiting to be indexed. */
    private Set<Timetable> dirty = new HashSet<Timetable>();
    
    /** 
     * Returns an updated timetable for the specified pattern if one is available in this snapshot, 
     * or the originally scheduled timetable if there are no updates in this snapshot. 
     */
    public Timetable resolve(TableTripPattern pattern) {
        Timetable timetable = timetables.get(pattern);
        if (timetable == null) {
            return pattern.scheduledTimetable;
        } else {
            LOG.trace("returning modified timetable");
            return timetable;
        }
    }
    
    /**
     * @return whether or not the update was actually applied
     */
    public boolean update(TableTripPattern pattern, UpdateBlock block) {
        // synchronization prevents commits/snapshots while update is in progress
        synchronized(this) {  
            if (dirty == null)
                throw new ConcurrentModificationException("This TimetableResolver is read-only.");
            Timetable tt = resolve(pattern);
            // we need to perform the copy of Timetable here rather than in Timetable.update()
            // to avoid repeatedly copying in case several updates are applied to the same timetable
            if ( ! dirty.contains(tt)) {
                tt = tt.copy();
                timetables.put(pattern, tt);
                dirty.add(tt);
            }        
            return tt.update(block);
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
    @SuppressWarnings("unchecked")
    public TimetableResolver commit() {
        TimetableResolver ret = new TimetableResolver();
        // synchronization prevents updates while commit/snapshot in progress
        synchronized(this) {
            if (! this.isDirty())
                return null;
            for (Timetable tt : dirty)
                tt.finish(); // summarize, index, etc. the new timetables
            ret.timetables = (HashMap<TableTripPattern, Timetable>) this.timetables.clone();
            this.dirty.clear();
        }
        ret.dirty = null; // mark the snapshot as henceforth immutable
        return ret;
    }
    
    public String toString() {
        String d = dirty == null ? "committed" : String.format("%d dirty", dirty.size());
        return String.format("Timetable snapshot: %d timetables (%s)", timetables.size(), d);
    }
    
    public boolean isDirty() {
        if (dirty == null)
            return false;
        return dirty.size() > 0;
    }
    
}
