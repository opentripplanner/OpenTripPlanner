package org.opentripplanner.updater.siri.updater;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

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
  implements
    PollingGraphUpdaterParameters, UrlUpdaterParameters, SiriETHttpTripUpdateSource.Parameters {
  public SiriETHttpTripUpdateSource.Parameters sourceParameters() {
    return this;
  }
}
