package org.opentripplanner.routing.edgetype;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.routing.edgetype.TableTripPattern.Timetable;

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
public class TimetableSnapshot {
    
    // Use HashMap not Map so we can clone.
    private HashMap<TableTripPattern, Timetable> timetables = new HashMap<TableTripPattern, Timetable>(); 
    
    private Set<Timetable> dirty = new HashSet<Timetable>();
    
    /** Returns an updated timetable for the specified pattern if one is available in this snapshot, 
     * or the originally scheduled timetable if there are no updates forin this snapshot. */
    public Timetable resolve(TableTripPattern pattern) {
        Timetable timetable = timetables.get(pattern);
        if (timetable == null)
            return pattern.scheduledTimetable;
        else
            return timetable;
    }
    
    protected Timetable modify(TableTripPattern pattern) {
        // check if this snapshot is already sealed in stone
        if (dirty == null) 
            throw new ConcurrentModificationException();
        Timetable existing = resolve(pattern);
        if (dirty.contains(existing)) {
            return existing;
        } else {
            Timetable fresh = existing.copy();
            timetables.put(pattern, fresh);
            dirty.add(fresh);
            return fresh;
        }        
    }
    
    protected void doneModifying() {
        // summarize, index, etc. the new timetables
        for (Timetable tt : dirty)
            tt.finish();
        // mark this snapshot as henceforth immutable
        dirty = null;
    }
    
    @SuppressWarnings("unchecked")
    public TimetableSnapshot mutableCopy() {
        TimetableSnapshot ret = new TimetableSnapshot();
        ret.timetables = (HashMap<TableTripPattern, Timetable>) this.timetables.clone();
        return ret;
    }
    
}
