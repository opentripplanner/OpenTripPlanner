package org.opentripplanner.transit.raptor.transitadapter;

import com.conveyal.r5.transit.TripPattern;
import com.conveyal.r5.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;

public class TripScheduleAdapter implements TripScheduleInfo {
    private final TripPattern tripPattern;
    private final TripSchedule schedule;

    TripScheduleAdapter(TripPattern tripPattern, TripSchedule schedule) {
        this.tripPattern = tripPattern;
        this.schedule = schedule;
    }

    int serviceCode() {
        return schedule.serviceCode;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return schedule.arrivals[stopPosInPattern];
    }

    @Override
    public int departure(int stopPosInPattern) {
        return schedule.departures[stopPosInPattern];
    }

    @Override
    public String debugInfo() {
        return tripPattern.routeId;
    }

    public TripPattern tripPattern() {
        return tripPattern;
    }

    public TripSchedule tripSchedule() {
        return schedule;
    }

    boolean isScheduledService() {
        return schedule.headwaySeconds == null;
    }

}
