package org.opentripplanner.routing.trippattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateList {
    
    private static final Logger LOG = LoggerFactory.getLogger(UpdateList.class);

    public final AgencyAndId tripId;

    public long timestamp; /// addme
    
    public final List<Update> updates;
    
    public void addUpdate(Update u) {
        this.updates.add(u);
    }
    
    public UpdateList(AgencyAndId tripId) {
        this.tripId = tripId;
        updates = new ArrayList<Update>();
    }
    
    public List<UpdateList> splitByTrip() {
        List<UpdateList> ret = new LinkedList<UpdateList>();
        // Update comparator sorts on tripId
        Collections.sort(updates);
        UpdateList ul = null;
        for (Update u : updates) {
            if (ul == null || ! ul.tripId.equals(u.tripId)) {
                ul = new UpdateList(u.tripId);
                ret.add(ul);
            }
            ul.updates.add(u);
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("tripId: ");
        sb.append(this.tripId);
        sb.append('\n');
        for (Update u : updates) {
            sb.append(u.toString());
            sb.append('\n');
        }
        return sb.toString();
    }

    /** Check that this UpdateList is internally coherent. */
    public boolean isSane() {
        // check that all Updates' trip_ids are the same, and match the UpdateList's trip_id
        for (Update u : updates)
            if (u == null || ! u.tripId.equals(this.tripId))
               return false;

        // check that sequence numbers are sequential and increasing
        boolean increasing = true;
        boolean sequential = true;
        boolean timesCoherent = true;
        Update prev_u = null;
        for (Update u : updates) {
            if (u.depart < u.arrive && u.depart != 0) // last update in trip may have 0 departure
                timesCoherent = false;
            if (prev_u != null) {
                if (u.stopSeq < prev_u.stopSeq)
                    increasing = false;
                if (u.stopSeq - prev_u.stopSeq != 1)
                    sequential = false;
                if (u.arrive < prev_u.depart)
                    timesCoherent = false;
            }
            prev_u = u;
        }
        if (!increasing || !timesCoherent) // || !sequential)
            return false;
        else
            return true;
    }

    /**        
     * Unfortunately updates cover subsets of the scheduled stop times, and these update blocks 
     * are not right-aligned wrt the full trip. They are contiguous, and delay predictions decay 
     * linearly to match scheduled times at the end of the block of updates.
     * 
     * TODO: verify: does this mean that we can use scheduled times for the rest of the trip? Or
     * are updates cumulative? 
     * 
     * Note that GTFS sequence number is increasing but not necessarily sequential.
     * Though most NL data providers use increasing, sequential values, Arriva Line 315 does not.
     * 
     * OTP does not store stop sequence numbers, since they could potentially be different for each
     * trip in a pattern. Maybe we should, and just reuse the array when they are the same, and set
     * it to null when they are increasing and sequential.
     * 
     * StopIds cannot be used to match update blocks because routes may contain loops with the same
     * stop appearing twice.
     * 
     * Because of all this we need to do some matching.
     * This method also verifies that the stopIds match those in the trip, as redundant error checking.
     * 
     * @param pattern
     * @return
     */
    public int findUpdateStopIndex(TableTripPattern pattern) {
        if (updates == null || updates.size() < 1)
            return -1;
        List<Stop> patternStops = pattern.getStops();
        // because seq numbers are positive (0?) and increasing, match index cannot be < first stopSeq
        Update firstUpdate = updates.get(0);
        int low = firstUpdate.stopSeq - 1; 
        int high = patternStops.size() - updates.size();
        PATTERN: for (int pi = low; pi <= high; pi++) { // index in pattern
            for (int ui = 0; ui < updates.size(); ui++) { // index in update
                Stop ps = patternStops.get(pi + ui);
                Update u = updates.get(ui);
                if ( ! ps.getId().getId().equals(u.stopId)) {
                    // stopId match failed
                    continue PATTERN;
                }
            }
            // stopId match succeeded
            LOG.trace("found matching stop block at index {}", pi);
            return pi;
        }
        return -1;
    }
}