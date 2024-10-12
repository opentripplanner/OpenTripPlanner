package org.opentripplanner.transit.speed_test.model.timer;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.cumulative.CumulativeCounter;
import io.micrometer.core.instrument.cumulative.CumulativeTimer;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramGauges;
import io.micrometer.core.instrument.distribution.pause.PauseDetector;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import java.time.Duration;
import java.util.Optional;

class MeterRegistrySetup {

  public static final String influxPasswordEnvVariable = "PERFORMANCE_INFLUX_DB_PASSWORD";

  public static Optional<MeterRegistry> getRegistry() {
    return influxPassword()
      .map(password -> {
        System.err.println(
          "Selecting InfluxDB as metrics registry. Sending data at end of speed test."
        );
        return MeterRegistrySetup.influxRegistry(password);
      });
  }

  static Optional<String> influxPassword() {
    return Optional.ofNullable(System.getenv(influxPasswordEnvVariable)).filter(s -> !s.isBlank());
  }

  static MeterRegistry influxRegistry(String password) {
    var influxConfig = new InfluxConfig() {
      @Override
      public Duration step() {
        // we don't want to periodically send results, we do it manually at the end of the test
        return Duration.ofDays(365);
      }

      @Override
      public String db() {
        return "performance";
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
      public String uri() {
        return "https://db.otp-performance.leonard.io";
      }

      @Override
      public String get(String k) {
        return null;
      }
    };

    return new CustomInfluxRegistry(influxConfig, Clock.SYSTEM);
  }

  /**
   * So the regular InfluxDB doesn't compute the mean times when you close the registry at the end
   * of the test run because it converts every timer into an instance of StepTimer. We actually want
   * to have a CumulativeTimer which collects the avg as it goes along, not at the end of the step
   * time.
   */
  public static class CustomInfluxRegistry extends InfluxMeterRegistry {

    public CustomInfluxRegistry(InfluxConfig config, Clock clock) {
      super(config, clock);
    }

    public void doPublish() {
      publish();
    }

    @Override
    protected Timer newTimer(
      Id id,
      DistributionStatisticConfig distributionStatisticConfig,
      PauseDetector pauseDetector
    ) {
      Timer timer = new CumulativeTimer(
        id,
        clock,
        distributionStatisticConfig,
        pauseDetector,
        getBaseTimeUnit()
      );
      HistogramGauges.registerWithCommonFormat(timer, this);
      return timer;
    }

    @Override
    protected Counter newCounter(Id id) {
      return new CumulativeCounter(id);
    }
  }
}
