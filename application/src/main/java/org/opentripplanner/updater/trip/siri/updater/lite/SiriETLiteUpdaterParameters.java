package org.opentripplanner.updater.trip.siri.updater.lite;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.trip.siri.updater.SiriETUpdaterParameters;

public record SiriETLiteUpdaterParameters(
  String configRef,
  String feedId,
  URI uri,
  Duration frequency,
  Duration timeout,
  boolean fuzzyTripMatching,
  HttpHeaders httpRequestHeaders
)
  implements SiriETUpdaterParameters, SiriETLiteHttpTripUpdateSource.Parameters {
  @Override
  public String url() {
    return uri.toString();
  }

  @Override
  public boolean blockReadinessUntilInitialized() {
    return false;
  }
}
