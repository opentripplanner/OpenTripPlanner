package org.opentripplanner.routing.trippattern;

import lombok.AllArgsConstructor;
import lombok.Delegate;
import lombok.NonNull;

/** 
 * Extend this class to wrap scheduled trip times yielding updated/patched/modified ones. 
 * This gets around a current limitation in Project Lombok where delegated methods cannot be 
 * overridden (issue 238).
 */
@AllArgsConstructor
public class DelegatingTripTimes implements TripTimes {

    @NonNull @Delegate(types=TripTimes.class)
    private final ScheduledTripTimes tt;
    
    public String toString() {
        return TripTimesUtil.toString(this);
    }
    
}
