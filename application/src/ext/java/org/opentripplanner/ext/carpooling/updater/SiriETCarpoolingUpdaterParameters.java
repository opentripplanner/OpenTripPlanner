package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.SiriETHttpTripUpdateSource;

public record SiriETCarpoolingUpdaterParameters(
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
  implements SiriETCarpoolingUpdater.Parameters, SiriETHttpTripUpdateSource.Parameters {
  public SiriETHttpTripUpdateSource.Parameters sourceParameters() {
    return this;
  }
}
