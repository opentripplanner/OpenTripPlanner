package org.opentripplanner.transit.raptor.speed_test.model.timer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opentripplanner.api.resource.DebugOutput;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

public class SpeedTestTimer {
    // only useful for the console
    private static final String SPEED_TEST_ROUTE = "speedTest.route";

    // router
    private static final String ROUTE_WORKER = "speedTest.route.worker";
    private static final String STREET_ROUTE = "speedTest.street.route";
    private static final String DIRECT_STREET_ROUTE = "speedTest.direct.street.route";
    private static final String TRANSIT_PATTERN_FILTERING = "speedTest.transit.data";
    private static final String TRANSIT_ROUTING = "speedTest.transit.routing";
    private static final String RAPTOR_SEARCH = "speedTest.transit.raptor";
    private static final String COLLECT_RESULTS = "speedTest.collect.results";

    private static final NamingConvention NAMING_CONVENTION = createNamingConvention();

    private static final long NANOS_TO_MILLIS = 1000000;

    public record Result(String name, int min, int max, int mean, int totTime, int count, int meanFailure, int totTimeFailure, int countFailure) {}

    private final Clock clock = Clock.SYSTEM;
    private final MeterRegistry loggerRegistry = new SimpleMeterRegistry();
    private final CompositeMeterRegistry registry = new CompositeMeterRegistry(clock, List.of(loggerRegistry));
    private final MeterRegistry uploadRegistry = MeterRegistrySetup.getRegistry().orElse(null);

    private Timer testTimer;
    private Timer tcTimer;
    private Timer.Sample tcSample = null;
    private List<String> tcTags;

    public List<Result> getResults() {
        var results = new ArrayList<Result>();
        for (Meter meter : registry.getMeters()) {
            if(meter instanceof Timer
                    && ((Timer) meter).count() > 0
                    && !"false".equals(meter.getId().getTag("success"))
            ) {
                Timer okTimer = (Timer) meter;
                Timer failureTimer = registry.timer(
                        meter.getId().getName(),
                        Tags.of("success", "false")
                );
                results.add(
                        new Result(
                                getFullName(meter),
                                (int )okTimer.percentile(0.01, TimeUnit.MILLISECONDS),
                                (int)okTimer.max(TimeUnit.MILLISECONDS),
                                (int)okTimer.mean(TimeUnit.MILLISECONDS),
                                (int)okTimer.totalTime(TimeUnit.MILLISECONDS),
                                (int)okTimer.count(),
                                (int)failureTimer.mean(TimeUnit.MILLISECONDS),
                                (int)failureTimer.totalTime(TimeUnit.MILLISECONDS),
                                (int)failureTimer.count()
                    )
                );
            }
        }
        return results;
    }


    public void setUp() {
        var measurementEnv = Optional.ofNullable(System.getenv("MEASUREMENT_ENVIRONMENT")).orElse("local");
        registry.config().commonTags(List.of(
                Tag.of("measurement.environment", measurementEnv),
                Tag.of("git.commit", projectInfo().versionControl.commit),
                Tag.of("git.branch", projectInfo().versionControl.branch),
                Tag.of("git.buildtime", projectInfo().versionControl.buildTime)
        ));

        // record the lowest percentile of times
        //noinspection NullableProblems
        registry.config().meterFilter(
                new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                        return DistributionStatisticConfig.builder()
                                .percentiles(0.01)
                                .build()
                                .merge(config);
                    }
                }
        );

    }

    /** Called before each run with all test-cases */
    public void startTest() {
        // Clear registry after first run
        registry.clear();

        if (uploadRegistry != null) {
            registry.add(uploadRegistry);
        }
        testTimer = testTimer();
        clearTesetCaseTimers();
    }

    /** Called before each test-case */
    public void startTestCase(List<String> tags) {
        tcTags = tags;
        tcTimer = testCaseTimer(tags);
        tcSample = Timer.start(clock);
    }

    /** Called before each test-case */
    public int lapTestCaseOk() {
        return nanosToMillisecond(tcSample.stop(tcTimer));
    }

    public int lapTestCaseFailed() {
        if(tcSample == null) {
            return 0;
        }
        var builder = Timer.builder(SPEED_TEST_ROUTE);
        addTagsToTimer(tcTags, builder);
        var timer = builder.tag("success", "false").register(registry);

        int lapTime = nanosToMillisecond(tcSample.stop(timer));

        tcSample = null;
        return lapTime;
    }

    public void lapTest() {
        if (uploadRegistry != null) {
            registry.remove(uploadRegistry);
        }
    }

    public void finnishUp() {
        // close() sends the results to influxdb
        if(uploadRegistry != null) {
            uploadRegistry.close();
        }
    }

    public void recordResults(DebugOutput data, List<String> tags, boolean isFailure) {
        record(STREET_ROUTE, data.transitRouterTimes.accessEgressTime, tags, isFailure);
        record(TRANSIT_PATTERN_FILTERING, data.transitRouterTimes.tripPatternFilterTime, tags, isFailure);
        record(DIRECT_STREET_ROUTE, data.directStreetRouterTime, tags, isFailure);
        record(COLLECT_RESULTS, data.transitRouterTimes.itineraryCreationTime, tags, isFailure);
        record(RAPTOR_SEARCH, data.transitRouterTimes.raptorSearchTime, tags, isFailure);
        record(TRANSIT_ROUTING, data.transitRouterTime, tags, isFailure);
    }
    public Timer testCaseTimer(List<String> tags) {
        var builder = Timer.builder(SPEED_TEST_ROUTE);
        addTagsToTimer(tags, builder);
        return builder.register(registry);
    }

    public int routingWorkerMean() {
        return (int) routingWorker().mean(TimeUnit.MILLISECONDS);
    }

    public int totalTimerMean() {
        return (int) testTimer.mean(TimeUnit.MILLISECONDS);
    }

    private Timer testTimer() {
        return Timer.builder(SPEED_TEST_ROUTE).register(registry);
    }

    private Timer routingWorker() {
        return Timer.builder(ROUTE_WORKER).register(registry);
    }

    public int testTotalTimeMs() {
        return (int)testTimer.totalTime(TimeUnit.MILLISECONDS);
    }

    private void record(String name, long nanos, List<String> tags, boolean isFailure) {
        var timer = Timer.builder(name);
        addTagsToTimer(tags, timer);
        if(isFailure) {
            timer.tag("success", "false");
        }
        timer.register(registry).record(Duration.ofNanos(nanos));
    }

    private void addTagsToTimer(List<String> tags, Timer.Builder timer) {
        if (!tags.isEmpty()) {
            timer.tag("tags", String.join(" ", tags));
        }
    }

    private void clearTesetCaseTimers() {
        tcTimer = null;
        tcSample = null;
    }
    private int nanosToMillisecond(long nanos) {
        return (int) (nanos / NANOS_TO_MILLIS);
    }



    public List<String> listMeterNames() {
        return registry.getMeters().stream()
                .map(SpeedTestTimer::getFullName)
                .toList();
    }

    private static String getFullName(Meter timer) {
        return timer.getId().getConventionName(NAMING_CONVENTION) + " [" + timer.getId().getTag("tags") + "]";
    }

    private static NamingConvention createNamingConvention() {
        return new NamingConvention() {
            @SuppressWarnings("NullableProblems")
            @Override
            public String name(String name, Meter.Type type, String unit) {
                return Arrays.stream(name.split("\\."))
                        .filter(Objects::nonNull)
                        .map(this::capitalize)
                        .collect(Collectors.joining(" "));
            }

            private String capitalize(String name) {
                if (name.length() != 0 && !Character.isUpperCase(name.charAt(0))) {
                    char[] chars = name.toCharArray();
                    chars[0] = Character.toUpperCase(chars[0]);
                    return new String(chars);
                } else {
                    return name;
                }
            }
        };
    }
}
