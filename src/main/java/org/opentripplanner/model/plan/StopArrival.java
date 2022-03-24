package org.opentripplanner.model.plan;

import java.util.Calendar;
import org.opentripplanner.model.base.ToStringBuilder;


/**
 * This class is used to represent a stop arrival event mostly for intermediate visits to a stops
 * along a route.
 */
public class StopArrival {
    public final Place place;
    /**
     * The time the rider will arrive at the place.
     */
    public final Calendar arrival;

    /**
     * The time the rider will depart the place.
     */
    public final Calendar departure;

    /**
     * For transit trips, the stop index (numbered from zero from the start of the trip).
     */
    public final Integer stopPosInPattern;

    /**
     * For transit trips, the sequence number of the stop. Per GTFS, these numbers are increasing.
     */
    public final Integer gtfsStopSequence;

    public StopArrival(
            Place place,
            Calendar arrival,
            Calendar departure,
            Integer stopPosInPattern,
            Integer gtfsStopSequence
    ) {
        this.place = place;
        this.arrival = arrival;
        this.departure = departure;
        this.stopPosInPattern = stopPosInPattern;
        this.gtfsStopSequence = gtfsStopSequence;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(StopArrival.class)
                .addTimeCal("arrival",arrival)
                .addTimeCal("departure", departure)
                .addObj("place", place)
                .toString();
    }
}
