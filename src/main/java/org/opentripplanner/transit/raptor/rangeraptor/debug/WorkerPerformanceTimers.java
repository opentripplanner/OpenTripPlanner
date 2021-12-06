package org.opentripplanner.transit.raptor.rangeraptor.debug;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class WorkerPerformanceTimers {
    // Variables to track time spent
    private final Timer timerRoute;
    private final Timer timerByMinuteScheduleSearch;
    private final Timer timerByMinuteTransfers;

    public WorkerPerformanceTimers(String namePrefix, MeterRegistry registry) {
        timerRoute = Timer.builder("raptor." + namePrefix + ".route").register(registry);
        timerByMinuteScheduleSearch = Timer
                .builder("raptor." + namePrefix + ".minute.transit")
                .register(registry);
        timerByMinuteTransfers = Timer
                .builder("raptor." + namePrefix + ".minute.transfers")
                .register(registry);
    }

    public Timer timerRoute() {
        return timerRoute;
    }

    public Timer timerByMinuteScheduleSearch() {
        return timerByMinuteScheduleSearch;
    }

    public Timer timerByMinuteTransfers() {
        return timerByMinuteTransfers;
    }
}
