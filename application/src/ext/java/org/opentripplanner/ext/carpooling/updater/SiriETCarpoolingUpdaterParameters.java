package org.opentripplanner.ext.carpooling.updater;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.updater.trip.siri.updater.SiriETUpdater;

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
  implements SiriETUpdater.Parameters, SiriETHttpTripUpdateSource.Parameters {
  public SiriETHttpTripUpdateSource.Parameters sourceParameters() {
    return this;
  }
}
