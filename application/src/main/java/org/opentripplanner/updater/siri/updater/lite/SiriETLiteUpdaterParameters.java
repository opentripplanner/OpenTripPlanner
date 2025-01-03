package org.opentripplanner.updater.siri.updater.lite;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.updater.siri.updater.SiriETUpdater;
import org.opentripplanner.updater.spi.HttpHeaders;

public record SiriETLiteUpdaterParameters(
  String configRef,
  String feedId,
  URI uri,
  Duration frequency,
  Duration timeout,
  boolean fuzzyTripMatching,
  HttpHeaders httpRequestHeaders
)
  implements SiriETUpdater.Parameters, SiriETLiteHttpTripUpdateSource.Parameters {
  public SiriETLiteHttpTripUpdateSource.Parameters sourceParameters() {
    return this;
  }

  @Override
  public String url() {
    return uri.toString();
  }

  @Override
  public boolean blockReadinessUntilInitialized() {
    return false;
  }
}
