package org.opentripplanner.profile;

import com.google.common.collect.Lists;

import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RaptorWorkerTimetable implements Serializable {
	
	private static final Logger LOG = LoggerFactory.getLogger(RaptorWorkerTimetable.class);

    // TODO put stop indexes in array here
    // TODO serialize using deltas and variable-width from Protobuf libs

    int nTrips, nStops;
    private int[][] timesPerTrip;
    
    /** Times (0-based) for frequency trips */
    private int[][] frequencyTrips;
    
    /** Headways (seconds) for frequency trips, parallel to above. Note that frequency trips are unsorted. */
    private int[] headwaySecs;
    
    /** Start times (seconds since noon - 12h) for frequency trips */
    private int[] startTimes;
    
    /** End times for frequency trips */
    private int[] endTimes;
    
    /** slack required when boarding a transit vehicle */
    public static final int MIN_BOARD_TIME_SECONDS = 60;

    private RaptorWorkerTimetable(int nTrips, int nStops) {
        this.nTrips = nTrips;
        this.nStops = nStops;
        timesPerTrip = new int[nTrips][];
    }

    /**
     * Return the trip index within the pattern of the soonest departure at the given stop number, requiring at least
     * MIN_BOARD_TIME_SECONDS seconds of slack. 
     */
    public int findDepartureAfter(int stop, int time) {
        for (int trip = 0; trip < timesPerTrip.length; trip++) {
            if (getDeparture(trip, stop) > time + MIN_BOARD_TIME_SECONDS) {
                return trip;
            }
        }
        return -1;
    }

    public int getArrival (int trip, int stop) {
        return timesPerTrip[trip][stop * 2];
    }

    public int getDeparture (int trip, int stop) {
        return timesPerTrip[trip][stop * 2 + 1];
    }
    
    /**
     * Get the departure on frequency trip trip at stop stop after time time,
     * assuming worst-case headway if worstCase is true.
     */
    public int getFrequencyDeparture (int trip, int stop, int time, boolean worstCase) {
    	int timeToReachStop = frequencyTrips[trip][stop * 2 + 1];
    	
    	// move time forward if the frequency has not yet started.
    	if (timeToReachStop + startTimes[trip] > time)
    		time = timeToReachStop + startTimes[trip];
    	
    	if (time > timeToReachStop + endTimes[trip])
    		return -1;
    	
    	if (worstCase)
    		time += headwaySecs[trip];
    	
    	return time;
    }
    
    /**
     * Get the travel time (departure to arrival) on frequency trip trip, from stop from to stop to.  
     */
    public int getFrequencyTravelTime (int trip, int from, int to) {
    	return frequencyTrips[trip][to * 2] - frequencyTrips[trip][from * 2 + 1];
    }
    
    /**
     * Get the number of frequency trips on this pattern.
     */
    public int getFrequencyTripCount () {
    	return headwaySecs.length;
    }

    /** This is a factory function rather than a constructor to avoid calling the super constructor for rejected patterns. */
    public static RaptorWorkerTimetable forPattern (Graph graph, TripPattern pattern, TimeWindow window) {

        // Filter down the trips to only those running during the window
        // This filtering can reduce number of trips and run time by 80 percent
        BitSet servicesRunning = window.servicesRunning;
        List<TripTimes> tripTimes = Lists.newArrayList();
        TT: for (TripTimes tt : pattern.scheduledTimetable.tripTimes) {
            if (servicesRunning.get(tt.serviceCode) &&
                    tt.getScheduledArrivalTime(0) < window.to &&
                    tt.getScheduledDepartureTime(tt.getNumStops() - 1) >= window.from) {
                tripTimes.add(tt);
            }
        }
        
        // find frequency trips
        List<FrequencyEntry> freqs = Lists.newArrayList();
        for (FrequencyEntry fe : pattern.scheduledTimetable.frequencyEntries) {
        	if (servicesRunning.get(fe.tripTimes.serviceCode) &&
        			fe.getMinDeparture() < window.to &&
        			fe.getMaxArrival() > window.from
        			) {
        		// this frequency entry has the potential to be used
        		
        		if (fe.exactTimes) {
        			LOG.warn("Exact-times frequency trips not yet supported");
        			continue;
        		}
        			
        		
        		freqs.add(fe);
        	}
        }
        
        if (tripTimes.isEmpty() && freqs.isEmpty()) {
            return null; // no trips active, don't bother storing a timetable
        }
        

        // Sort the trip times by their first arrival time
        Collections.sort(tripTimes, new Comparator<TripTimes>() {
            @Override
            public int compare(TripTimes tt1, TripTimes tt2) {
                return (tt1.getScheduledArrivalTime(0) - tt2.getScheduledArrivalTime(0));
            }
        });

        // Copy the times into the compacted table
        RaptorWorkerTimetable rwtt = new RaptorWorkerTimetable(tripTimes.size(), pattern.getStops().size() * 2);
        int t = 0;
        for (TripTimes tt : tripTimes) {
            int[] times = new int[rwtt.nStops];
            for (int s = 0; s < pattern.getStops().size(); s++) {
                int arrival = tt.getScheduledArrivalTime(s);
                int departure = tt.getScheduledDepartureTime(s);
                times[s * 2] = arrival;
                times[s * 2 + 1] = departure;
            }
            rwtt.timesPerTrip[t++] = times;
        }
        
        // save frequency times
        rwtt.frequencyTrips = new int[freqs.size()][pattern.getStops().size() * 2];
        rwtt.endTimes = new int[freqs.size()];
        rwtt.startTimes = new int[freqs.size()];
        rwtt.headwaySecs = new int[freqs.size()];
        
        {
		    int i = 0;
		    for (FrequencyEntry fe : freqs) {
		    	rwtt.headwaySecs[i] = fe.headway;
		    	rwtt.startTimes[i] = fe.startTime;
		    	rwtt.endTimes[i] = fe.endTime;
		    	
		    	int[] times = rwtt.frequencyTrips[i];
		    	
		    	for (int s = 0; s < fe.tripTimes.getNumStops(); s++) {
		    		times[s * 2] = fe.tripTimes.getScheduledArrivalTime(s);
		    		times[s * 2 + 1] = fe.tripTimes.getScheduledDepartureTime(s);
		    	}
		    	
		    	i++;
		    }
        }
        
        return rwtt;
    }

}
