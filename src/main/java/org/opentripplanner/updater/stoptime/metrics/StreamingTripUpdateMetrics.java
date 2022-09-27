package org.opentripplanner.updater.stoptime.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.updater.stoptime.UpdateResult;
import org.opentripplanner.updater.stoptime.UrlUpdaterParameters;

public class StreamingTripUpdateMetrics extends TripUpdateMetrics {

  protected static final String METRICS_PREFIX = "streaming_trip_updates";
  private final Counter successfulCounter;
  private final Counter failureCounter;
  private final Map<UpdateError.UpdateErrorType, Counter> failuresByType = new HashMap<>();
  private static final Consumer<UpdateResult> NOOP = ignored -> {};

  public StreamingTripUpdateMetrics(UrlUpdaterParameters parameters) {
    super(parameters);
    this.successfulCounter = getCounter("successful");
    this.failureCounter = getCounter("failed");
  }

  public void setCounters(UpdateResult result) {
    this.successfulCounter.increment(result.successful());
    this.successfulCounter.increment(result.failed());

    for (var errorType : result.failures().keySet()) {
      var counter = failuresByType.get(errorType);
      if (Objects.isNull(counter)) {
        counter = getCounter("failure_type", Tag.of("errorType", errorType.name()));
        failuresByType.put(errorType, counter);
      }
      counter.increment(result.failures().get(errorType).size());
    }
  }

  private Counter getCounter(String name, Tag... tags) {
    var finalTags = Tags.concat(Arrays.stream(tags).toList(), baseTags);
    return Metrics.globalRegistry.counter(METRICS_PREFIX + "." + name, finalTags);
  }
}
