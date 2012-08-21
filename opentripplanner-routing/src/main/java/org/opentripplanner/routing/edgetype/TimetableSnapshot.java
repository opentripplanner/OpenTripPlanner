package org.opentripplanner.routing.edgetype;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.edgetype.TableTripPattern.Timetable;
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
public class TimetableSnapshot {
    
    private static final Logger LOG = LoggerFactory.getLogger(TimetableSnapshot.class);

    // Use HashMap not Map so we can clone.
    // if this turns out to be slow/spacious we can use an array with integer pattern indexes
    private HashMap<TableTripPattern, Timetable> timetables = new HashMap<TableTripPattern, Timetable>(); 
    
    private Set<Timetable> dirty = new HashSet<Timetable>();
    
    /** Returns an updated timetable for the specified pattern if one is available in this snapshot, 
     * or the originally scheduled timetable if there are no updates in this snapshot. */
    public Timetable resolve(TableTripPattern pattern) {
        Timetable timetable = timetables.get(pattern);
        if (timetable == null) {
            return pattern.scheduledTimetable;
        } else {
            //LOG.debug("returning modified timetable");
            return timetable;
        }
    }
    
    public Timetable modify(TableTripPattern pattern) {
        if (dirty == null) // this snapshot was already committed for reading
            throw new ConcurrentModificationException();
        Timetable existing = resolve(pattern);
        if (dirty.contains(existing)) {
            return existing; // allows modifying multiple trips on a single pattern
        } else {
            // OR fresh = pattern.scheduledTimetable.copy();
            Timetable fresh = existing.copy();
            timetables.put(pattern, fresh);
            dirty.add(fresh);
            return fresh;
        }        
    }
    
    public void commit() {
        // summarize, index, etc. the new timetables
        for (Timetable tt : dirty)
            tt.finish();
        // mark this snapshot as henceforth immutable
        dirty = null;
    }
    
    public String toString() {
        String d = dirty == null ? "committed" : String.format("%d dirty", dirty.size());
        return String.format("Timetable snapshot: %d timetables (%s)", timetables.size(), d);
    }
    
    @SuppressWarnings("unchecked")
    public TimetableSnapshot mutableCopy() {
        TimetableSnapshot ret = new TimetableSnapshot();
        ret.timetables = (HashMap<TableTripPattern, Timetable>) this.timetables.clone();
        return ret;
    }
    
}
