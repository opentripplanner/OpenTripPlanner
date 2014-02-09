package org.opentripplanner.profile;

import java.util.BitSet;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class TimeWindow {
    int from;
    int to;
    BitSet servicesRunning;
    boolean includes (int t) {
        return t > from && t < to;
    }
}