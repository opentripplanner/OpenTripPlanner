package org.opentripplanner.transit.raptor.speed_test;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import java.time.Duration;
import java.util.Optional;

public class RegistrySetup {

    public static final String influxPasswordEnvVariable = "PERFORMANCE_INFLUX_DB_PASSWORD";

    static Optional<String> influxPassword() {
        return Optional.ofNullable(System.getenv(influxPasswordEnvVariable));
    }

    static MeterRegistry influxRegistry(String password) {

        var influxConfig = new InfluxConfig() {

            @Override
            public Duration step() {
                // we intentionally set a high step because we only want to send the metrics on shutdown
                return Duration.ofMinutes(10);
            }

            @Override
            public String db() {
                return "performance";
            }

            @Override
            public String uri() {
                return "https://otp-github-actions-runner.leonard.io:8087";
            }

            @Override
            public String userName() {
                return "performance";
            }

            @Override
            public String password() {
                return password;
            }

            @Override
            public String get(String k) {
                return null;
            }
        };

        System.out.println(influxConfig.uri());
        System.out.println(influxConfig.password());
        System.out.println(password);
        return new InfluxMeterRegistry(influxConfig, Clock.SYSTEM);
    }

    static MeterRegistry chooseRegistry() {
        return influxPassword()
                .map(password -> {
                    System.err.println("Selecting InfluxDB as metrics registry. Sending data at end of speed test.");
                    return RegistrySetup.influxRegistry(password);
                })
                .orElseGet(() -> {
                    System.err.println("Environment variable " + influxPasswordEnvVariable
                            + " not set. Not sending result data to InfluxDB.");
                    return new SimpleMeterRegistry();
                });
    }
}
