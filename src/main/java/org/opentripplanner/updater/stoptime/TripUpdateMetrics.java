package org.opentripplanner.updater.stoptime;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.opentripplanner.model.UpdateError;
import org.opentripplanner.util.OTPFeature;

public class TripUpdateMetrics {

  static final String METRICS_PREFIX = "trip_updates";
  private final List<Tag> baseTags;
  private final AtomicInteger successfulGauge;
  private final AtomicInteger failureGauge;
  private final Map<UpdateError.UpdateErrorType, AtomicInteger> failuresByType = new HashMap<>();

  public TripUpdateMetrics(PollingTripUpdaterParameters parameters) {
    this.baseTags =
      List.of(
        Tag.of("configRef", parameters.getConfigRef()),
        Tag.of("url", parameters.httpSourceParameters().getUrl()),
        Tag.of("feedId", parameters.getFeedId())
      );

    this.successfulGauge = getGauge("successful");
    this.failureGauge = getGauge("failed");
  }

  public static Consumer<UpdateResult> buildConsumer(PollingTripUpdaterParameters parameters) {
    if (OTPFeature.ActuatorAPI.isOn()) {
      var metrics = new TripUpdateMetrics(parameters);
      return metrics::setGauges;
    } else {
      return ignored -> {};
    }
  }

  public void setGauges(UpdateResult result) {
    this.successfulGauge.set(result.successful());
    this.failureGauge.set(result.failed());

    for (var errorType : result.failures().keySet()) {
      var counter = failuresByType.get(errorType);
      if (Objects.isNull(counter)) {
        counter = getGauge("failure_type", Tag.of("errorType", errorType.name()));
        failuresByType.put(errorType, counter);
      }
      counter.set(result.failures().get(errorType).size());
    }

    var toZero = new HashSet<>(failuresByType.keySet());
    toZero.removeAll(result.failures().keySet());

    for (var keyToZero : toZero) {
      failuresByType.get(keyToZero).set(0);
    }
  }

  private AtomicInteger getGauge(String name, Tag... tags) {
    var finalTags = Tags.concat(Arrays.stream(tags).toList(), baseTags);
    return Metrics.globalRegistry.gauge(
      METRICS_PREFIX + "." + name,
      finalTags,
      new AtomicInteger(0)
    );
  }
}
