package org.opentripplanner.updater.trip.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.updater.trip.UpdateResult;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

/**
 * Records micrometer metrics for trip updaters that send batches of updates, for example GTFS-RT
 * via HTTP.
 * <p>
 * It records the most recent trip update as gauges.
 */
public class BatchTripUpdateMetrics extends TripUpdateMetrics {

  protected static final String METRICS_PREFIX = "batch_trip_updates";
  private final AtomicInteger successfulGauge;
  private final AtomicInteger failureGauge;
  private final Map<UpdateError.UpdateErrorType, AtomicInteger> failuresByType = new HashMap<>();

  public BatchTripUpdateMetrics(UrlUpdaterParameters parameters) {
    super(parameters);
    this.successfulGauge =
      getGauge(
        "successful",
        "Trip updates that were successfully applied at the most recent update"
      );
    this.failureGauge =
      getGauge("failed", "Trip updates that failed to apply at the most recent update");
  }

  public void setGauges(UpdateResult result) {
    this.successfulGauge.set(result.successful());
    this.failureGauge.set(result.failed());

    for (var errorType : result.failures().keySet()) {
      var counter = failuresByType.get(errorType);
      if (Objects.isNull(counter)) {
        counter =
          getGauge(
            "failure_type",
            "Failure types of the most recent update",
            Tag.of("errorType", errorType.name())
          );
        failuresByType.put(errorType, counter);
      }
      counter.set(result.failures().get(errorType).size());
    }

    // every counter that was set in one of the previous rounds but not in this one
    // needs to be explicitly set to zero, otherwise the previous count will persist across
    // batches. this would of course lead to wrong totals.
    var toZero = new HashSet<>(failuresByType.keySet());
    toZero.removeAll(result.failures().keySet());

    for (var keyToZero : toZero) {
      failuresByType.get(keyToZero).set(0);
    }
  }

  private AtomicInteger getGauge(String name, String description, Tag... tags) {
    var finalTags = Tags.concat(Arrays.stream(tags).toList(), baseTags);
    var atomicInt = new AtomicInteger(0);
    Gauge
      .builder(METRICS_PREFIX + "." + name, atomicInt::get)
      .description(description)
      .tags(finalTags)
      .register(Metrics.globalRegistry);
    return atomicInt;
  }
}
