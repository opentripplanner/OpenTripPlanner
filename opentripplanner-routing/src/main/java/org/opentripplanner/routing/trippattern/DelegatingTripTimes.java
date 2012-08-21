package org.opentripplanner.routing.trippattern;

import lombok.AllArgsConstructor;
import lombok.Delegate;
import lombok.NonNull;

/** 
 * Extend this class to wrap scheduled trip times yielding updated/patched/modified ones. 
 * This gets around a current limitation in Project Lombok where delegated methods cannot be 
 * overridden.  
 */
@AllArgsConstructor
public class DelegatingTripTimes {

    @NonNull @Delegate
    private final TripTimes tt;
    
}
