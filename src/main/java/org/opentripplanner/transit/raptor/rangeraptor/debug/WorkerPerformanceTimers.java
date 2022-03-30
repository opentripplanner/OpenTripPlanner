package org.opentripplanner.transit.raptor.rangeraptor.debug;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Collection;
import org.opentripplanner.routing.framework.MicrometerUtils;

public class WorkerPerformanceTimers {
    // Variables to track time spent
    private final Timer timerRoute;
    private final Timer timerByMinuteScheduleSearch;
    private final Timer timerByMinuteTransfers;

    public WorkerPerformanceTimers(
            String namePrefix,
            Collection<String> timingTags,
            MeterRegistry registry
    ) {
        var tags = MicrometerUtils.mapTimingTags(timingTags);
        timerRoute = Timer
                .builder("raptor." + namePrefix + ".route")
                .tags(tags)
                .register(registry);
        timerByMinuteScheduleSearch = Timer
                .builder("raptor." + namePrefix + ".minute.transit")
                .tags(tags)
                .register(registry);
        timerByMinuteTransfers = Timer
                .builder("raptor." + namePrefix + ".minute.transfers")
                .tags(tags)
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
