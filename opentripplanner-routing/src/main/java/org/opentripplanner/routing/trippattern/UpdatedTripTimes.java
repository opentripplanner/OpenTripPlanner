package org.opentripplanner.routing.trippattern;

/**
 * An UpdatedTripTimes applies updated arrival and departure times to a subset of a trip's stops,
 * reading through to the scheduled TripTimes for subsequent stops, and reporting that the vehicle
 * has passed stops preceding the update block.
 */
public class UpdatedTripTimes extends DelegatingTripTimes implements TripTimes {

    private final int offset;
    
    private int[] arrivals;
    
    private int[] departures;
    
    // maybe push pattern and offset into block
    public UpdatedTripTimes(ScheduledTripTimes sched, UpdateBlock block, int offset) {
        super(sched);
        this.offset = offset;
        int nStops = block.updates.size();
        this.arrivals = new int[nStops];
        this.departures = new int[nStops];
        for (Update u : block.updates) {
            arrivals[offset] = u.arrive;
            departures[offset] = u.depart;
            offset += 1;
        }
        this.compact();
    }

    @Override public int getDepartureTime(int hop) {
        int stop = hop;
        if (stop < offset)
            return TripTimes.PASSED;
        if (stop >= offset + departures.length)
            return super.getDepartureTime(hop);
        return departures[stop];
    }
    
    @Override public int getArrivalTime(int hop) {
        int stop = hop + 1;
        if (stop < offset)
            return TripTimes.PASSED;
        if (stop >= offset + departures.length)
            return super.getArrivalTime(hop);
        if (arrivals == null)
            return departures[stop];
        return arrivals[stop];
    }
    
    @Override public boolean compact() {
        if (arrivals == null)
            return false;
        for (int i = 0; i < arrivals.length; i++) {
            if (arrivals[i] != departures[i]) {
                return false;
            }
        }
        arrivals = null;
        return true;
    }
    
}
