package org.opentripplanner.updater.trip.siri.updater;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;

public record SiriETUpdaterParameters(
  String configRef,
  String feedId,
  boolean blockReadinessUntilInitialized,
  String url,
  Duration frequency,
  String requestorRef,
  Duration timeout,
  Duration previewInterval,
  boolean fuzzyTripMatching,
  HttpHeaders httpRequestHeaders,
  boolean producerMetrics
)
  implements SiriETUpdater.Parameters, SiriETHttpTripUpdateSource.Parameters {
  public SiriETHttpTripUpdateSource.Parameters sourceParameters() {
    return this;
  }
}
