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
        int nUpdates = block.updates.size();
        this.arrivals = new int[nUpdates];
        this.departures = new int[nUpdates];
        int ui = 0;
        for (Update update : block.updates) {
            arrivals[ui] = update.arrive;
            departures[ui] = update.depart;
            ui += 1;
        }
        this.compact();
    }

    @Override public int getDepartureTime(int hop) {
        int stop = hop;
        int update = stop - offset;
        if (update < 0)
            return TripTimes.PASSED;
        if (update >= departures.length)
            return super.getDepartureTime(hop);
        return departures[update];
    }
    
    @Override public int getArrivalTime(int hop) {
        int stop = hop + 1;
        int update = stop - offset;
        if (update < 0)
            return TripTimes.PASSED;
        if (update >= departures.length)
            return super.getArrivalTime(hop);
        if (arrivals == null)
            return departures[update];
        return arrivals[update];
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
    
    public String toString() {
        String s = String.format("UpdatedTripTimes block size %d at stop %d\n", 
                departures.length, offset);
        return s + super.toString();
    }

}
