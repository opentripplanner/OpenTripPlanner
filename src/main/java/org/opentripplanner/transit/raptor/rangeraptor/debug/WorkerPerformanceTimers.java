package org.opentripplanner.transit.raptor.rangeraptor.debug;

import org.opentripplanner.transit.raptor.util.AvgTimer;

public class WorkerPerformanceTimers {
    // Variables to track time spent
    private final AvgTimer timerRoute;
    private final AvgTimer timerRouteSetup;
    private final AvgTimer timerRouteByMinute;
    private final AvgTimer timerByMinuteScheduleSearch;
    private final AvgTimer timerByMinuteTransfers;

    public WorkerPerformanceTimers(String namePrefix) {
        timerRoute = AvgTimer.timerMilliSec(namePrefix + ":route");
        timerRouteSetup = AvgTimer.timerMilliSec(namePrefix + ":route Init");
        timerRouteByMinute = AvgTimer.timerMilliSec(namePrefix + ":route For Minute");
        timerByMinuteScheduleSearch = AvgTimer.timerMicroSec(namePrefix + ":runRaptorForMinute Transit");
        timerByMinuteTransfers = AvgTimer.timerMicroSec(namePrefix + ":runRaptorForMinute Transfers");
    }

    public AvgTimer timerRoute() {
        return timerRoute;
    }

    public void timerSetup(Runnable setup) {
        timerRouteSetup.time(setup);
    }

    public void timerRouteByMinute(Runnable routeByMinute) {
        timerRouteByMinute.time(routeByMinute);
    }

    public AvgTimer timerByMinuteScheduleSearch() {
        return timerByMinuteScheduleSearch;
    }

    public AvgTimer timerByMinuteTransfers() {
        return timerByMinuteTransfers;
    }
}
