package org.opentripplanner.updater.siri.updater.light;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.updater.siri.updater.SiriUpdaterParameters;
import org.opentripplanner.updater.spi.HttpHeaders;

public record SiriETLightUpdaterParameters(
  String configRef,
  String feedId,
  URI uri,
  Duration frequency,
  Duration timeout,
  boolean fuzzyTripMatching,
  HttpHeaders httpRequestHeaders
)
  implements SiriUpdaterParameters, SiriETLightHttpTripUpdateSource.Parameters {
  public SiriETLightHttpTripUpdateSource.Parameters sourceParameters() {
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
