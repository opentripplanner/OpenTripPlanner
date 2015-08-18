package org.opentripplanner.profile;

import java.time.DayOfWeek;
import java.util.BitSet;

public class TimeWindow {
    int from;
    int to;
    BitSet servicesRunning;
    DayOfWeek dayOfWeek;

    public TimeWindow(int from, int to, BitSet servicesRunning) {
        this(from, to, servicesRunning, null);
    }

    public TimeWindow(int from, int to, BitSet servicesRunning, DayOfWeek dayOfWeek) {
		this.from = from;
		this.to = to;
		this.servicesRunning = servicesRunning;
        this.dayOfWeek = dayOfWeek;
	}
    
	boolean includes (int t) {
        return t > from && t < to;
    }

    /* @return the number of seconds in the given interval overlapping this window. */
    int overlap(int from2, int to2, int serviceCode) {
        if ( ! servicesRunning.get(serviceCode)) return 0;
        if (from2 > to || to2 < from) return 0;
        int max_from = Math.max(from, from2);
        int min_to = Math.min(to, to2);
        return min_to - max_from;
    }

}