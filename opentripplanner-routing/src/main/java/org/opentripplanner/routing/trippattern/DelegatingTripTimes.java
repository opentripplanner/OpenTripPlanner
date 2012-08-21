package org.opentripplanner.routing.trippattern;

import org.onebusaway.gtfs.model.Trip;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/** 
 * Extend this class to wrap scheduled trip times, yielding updated/patched/modified ones. 
 */
@AllArgsConstructor
public abstract class DelegatingTripTimes extends TripTimes {

    @NonNull private final ScheduledTripTimes base;

    @Override public Trip getTrip() { return base.getTrip(); }

    @Override public ScheduledTripTimes getScheduledTripTimes() { return base.getScheduledTripTimes(); }

    @Override public int getNumHops() { return base.getNumHops(); }

    @Override public int getDepartureTime(int hop) { return base.getDepartureTime(hop); }

    @Override public int getArrivalTime(int hop) { return base.getArrivalTime(hop); }
 
    @Override public String toString() { return base.toString(); }

}
