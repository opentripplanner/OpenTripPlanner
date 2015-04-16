package org.opentripplanner.routing.trippattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.joda.time.LocalDate;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This is an alternative representation of all the TripTimes in a single Graph that are running during a particular
 * time range on a particular day.
 * It allows for much faster spatial analysis (via the RAPTOR algorithm) and profile routing, because it places all
 * data for a single TripPattern in a contiguous region of memory, and because pre-filtering the TripTimes eliminates
 * the checks that need to be performed when we search for the soonest departure.
 * Unlike in "normal" OTP searches, we assume non-overtaking (FIFO) vehicle behavior within a single TripPattern,
 * which is generally the case in clean input data. The key difference here is that profile routing and spatial analysis
 * do not need to take real-time (GTFS-RT) updates into account since they are intended to be generic results
 * describing a scenario in the future.
 */
public class TripTimeSubset {
	private static final Logger LOG = LoggerFactory.getLogger(TripTimeSubset.class);
	
	/**
	 * The times of this pattern.
	 * This is a one-dimensional array because in Java one-dimensional arrays are contiguous in memory.
	 *
	 * It is laid out as arrival time of trip 0 at stop 0, departure time of trip 0 at stop zero, arrival time of trip
	 * 0 at stop 1 ... departure time of trip n at stop k.
	 */
	private int[] times;
	
	/** The number of stops in a single trip */
	private int tripLength;
	
	/** The number of trips on this pattern */
	private int tripCount;
	
	public int getArrivalTime (int trip, int stop) {
		int idx = (trip * tripLength + stop) * 2;
		return times[idx];
	}
	
	public int getDepartureTime (int trip, int stop) {
		int idx = (trip * tripLength + stop) * 2 + 1;
		return times[idx];
	}
	
	/**
	 * Find the index of the trip that departs the given stop index after a given time, in seconds since midnight.
	 * This uses a binary search so it should be pretty fast.
	 * 
	 * This assumes that trips on a particular pattern are non-overtaking.
	 */ 
	public int findTripAfter (int stop, int time) {
		return findTripAfter (stop, time, 0, tripCount);
	}
	
	private int findTripAfter (int stop, int time, int minInclusive, int maxExclusive) {
		int idx = (minInclusive + maxExclusive) / 2;
		
		if (idx == 0 || idx == tripCount - 1) {
			return getArrivalTime(idx, stop) < time ? -1 : idx;
		}
		
		if (getArrivalTime(idx, stop) >= time && getArrivalTime(idx - 1, stop) < time)
			return idx;
		
		if (getArrivalTime(idx, stop) >= time)
			return findTripAfter(stop, time, minInclusive, idx);
		
		else
			return findTripAfter(stop, time, idx + 1, maxExclusive);
	}

	/**
	 * Create a TripTimeSubset from a given trip pattern, bitset of services running, and time window.
	 * All trips running at any time during the window will be included.
	 * This uses the scheduled times of the trip (i.e. no real time updates are taken into account).
	 */
	public static TripTimeSubset create(Graph graph, TripPattern tp, BitSet servicesRunning, int startTime, int endTime) {
		// filter down the trips
		List<TripTimes> tripTimes = Lists.newArrayList();
		
		for (TripTimes tt : tp.scheduledTimetable.tripTimes) {
			if (!servicesRunning.get(tt.serviceCode))
				continue;
			
			if (tt.scheduledArrivalTimes[0] + tt.timeShift <= endTime &&
					tt.scheduledDepartureTimes[tt.scheduledDepartureTimes.length - 1] + tt.timeShift >= startTime )
				tripTimes.add(tt);
		}
		
		if (tripTimes.isEmpty())
			return null;
		
		// sort the trip times by their first arrival time
		Collections.sort(tripTimes, new Comparator<TripTimes> () {

			@Override
			public int compare(TripTimes o1, TripTimes o2) {
				return (o1.scheduledArrivalTimes[0] + o1.timeShift) - (o2.scheduledArrivalTimes[0] + o2.timeShift);
			}
			
		});
		
		TripTimeSubset tts = new TripTimeSubset();
		tts.tripCount = tripTimes.size();
		tts.tripLength = tripTimes.get(0).scheduledArrivalTimes.length;
		tts.times = new int[tts.tripCount * tts.tripLength * 2];
		
		// fill the times
		int tripIdx = 0;
		for (TripTimes tt : tripTimes) {
			for (int stopIdx = 0; stopIdx < tts.tripLength; stopIdx++) {
				tts.times[(tripIdx * tts.tripLength + stopIdx) * 2] = tt.scheduledArrivalTimes[stopIdx] + tt.timeShift;
				tts.times[(tripIdx * tts.tripLength + stopIdx) * 2 + 1] = tt.scheduledDepartureTimes[stopIdx] + tt.timeShift;
				
				// ensure trips are non-overtaking, as otherwise the binary search won't work
				if (tripIdx >= 1) {
					if (tts.times[(tripIdx * tts.tripLength + stopIdx) * 2] <=
							tts.times[((tripIdx - 1) * tts.tripLength + stopIdx) * 2]) {
						LOG.warn("Overtaking/duplicate arrival times on trip pattern {}, trip {} at stop {}," +
							" trashing this pattern.", tp, tripIdx, stopIdx);
						return null;
					}
					
					
					if (tts.times[(tripIdx * tts.tripLength + stopIdx) * 2 + 1] <=
							tts.times[((tripIdx - 1) * tts.tripLength + stopIdx) * 2 + 1]) {
						LOG.warn("Overtaking/duplicate departure times on trip pattern {}, trip {} at stop {}," +
							" trashing this pattern.", tp, tripIdx, stopIdx);
						return null;
					}
				}
			}
			tripIdx++;
		}
		
		return tts;
	}

	/**
	 * Create TripTimeSubsets for every trip pattern in a graph that is running at a time window.
	 * These custom timetables enable some very effective optimizations in profile routing / spatial analysis which speed
	 * up repeated RAPTOR routing by 80 percent for example. However, despite yielding a significant speedup over
	 * the course of a full search, this initial timetable building operation is quite fast (a few milliseconds for
	 * Washington DC). Therefore there's no need to cache the timetables, we can just compute a custom timetable at the
	 * beginning of each search.
	 */
	public static Map<TripPattern, TripTimeSubset> indexGraph(Graph graph, LocalDate date, int startTime, int endTime) {
		return indexGraph(graph, graph.index.servicesRunning(date), startTime, endTime);
	}

	/** Create TripTimeSubsets for every trip pattern in a graph that is running at a time window */
	public static Map<TripPattern, TripTimeSubset> indexGraph(Graph graph, BitSet servicesRunning,
															  int startTime, int endTime) {
		Map<TripPattern, TripTimeSubset> ret = Maps.newHashMap();
		
		for (TripPattern tp : graph.index.patternForId.values()) {
			TripTimeSubset tts = create(graph, tp, servicesRunning, startTime, endTime);
			
			if (tts != null)
				ret.put(tp, tts);
		}
		
		return ret;
	}
}
