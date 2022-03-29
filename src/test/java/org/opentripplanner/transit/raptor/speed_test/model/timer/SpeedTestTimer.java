package org.opentripplanner.transit.raptor.speed_test.model.timer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

public class SpeedTestTimer {
    private static final NamingConvention NAMING_CONVENTION = createNamingConvention();

    private static final long NANOS_TO_MILLIS = 1000000;

    public record Result(String name, int min, int max, int mean, int totTime, int count) {}

    private final Clock clock = Clock.SYSTEM;
    private final MeterRegistry loggerRegistry = new SimpleMeterRegistry();
    private final CompositeMeterRegistry registry = new CompositeMeterRegistry(clock, List.of(loggerRegistry));
    private final MeterRegistry uploadRegistry = MeterRegistrySetup.getRegistry().orElse(null);

    public List<Result> getResults() {
        var results = new ArrayList<Result>();
        for (Meter meter : registry.getMeters()) {
            if(meter instanceof Timer timer && ((Timer) meter).count() > 0) {
                results.add(
                        new Result(
                                getFullName(meter),
                                (int)timer.percentile(0.01, TimeUnit.MILLISECONDS),
                                (int)timer.max(TimeUnit.MILLISECONDS),
                                (int)timer.mean(TimeUnit.MILLISECONDS),
                                (int)timer.totalTime(TimeUnit.MILLISECONDS),
                                (int)timer.count()
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

    public MeterRegistry getRegistry() {
        return registry;
    }

    /** Called before each run with all test-cases */
    public void startTest() {
        // Clear registry after first run
        registry.clear();

        if (uploadRegistry != null) {
            registry.add(uploadRegistry);
        }
    }

    public void lapTest() {
        if (uploadRegistry != null) {
            registry.remove(uploadRegistry);
        }
    }

    public void finishUp() {
        // close() sends the results to influxdb
        if(uploadRegistry != null) {
            uploadRegistry.close();
        }
    }

    public int totalTimerMean(String timerName) {
        long count = getTotalTimers(timerName).mapToLong(Timer::count).sum();
        return (int) (testTotalTimeMs(timerName) / count);
    }

    public int testTotalTimeMs(String timerName) {
        return getTotalTimers(timerName)
                .mapToInt(timer -> (int) timer.totalTime(TimeUnit.MILLISECONDS))
                .sum();
    }

    private Stream<Timer> getTotalTimers(String timerName) {
        return registry.find(timerName)
                .meters()
                .stream()
                .filter(Timer.class::isInstance)
                .map(Timer.class::cast);
    }

    public static int nanosToMillisecond(long nanos) {
        return (int) (nanos / NANOS_TO_MILLIS);
    }

    private static String getFullName(Meter timer) {
        var sb = new StringBuilder(timer.getId().getConventionName(NAMING_CONVENTION));
        if (timer.getId().getTag("tags") != null) {
            sb.append(" [");
            sb.append(timer.getId().getTag("tags"));
            sb.append("]");
        }

        return sb.toString();
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
