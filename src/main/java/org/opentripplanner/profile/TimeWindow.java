package org.opentripplanner.profile;

import java.util.BitSet;

class TimeWindow {
    int from;
    int to;
    BitSet servicesRunning;
    
    public TimeWindow(int from, int to, BitSet servicesRunning) {
		this.from = from;
		this.to = to;
		this.servicesRunning = servicesRunning;
	}
    
	boolean includes (int t) {
        return t > from && t < to;
    }
}