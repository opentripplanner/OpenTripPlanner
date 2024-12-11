package org.opentripplanner.updater.siri.updater.lite;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.updater.siri.updater.SiriSXUpdater;
import org.opentripplanner.updater.spi.HttpHeaders;

public record SiriSXLiteUpdaterParameters(
  String configRef,
  String feedId,
  URI uri,
  Duration frequency,
  Duration earlyStart,
  Duration timeout,
  HttpHeaders requestHeaders
)
  implements SiriSXUpdater.Parameters {
  @Override
  public String requestorRef() {
    return "OpenTripPlanner";
  }

  @Override
  public boolean blockReadinessUntilInitialized() {
    return false;
  }

  @Override
  public String url() {
    return uri.toString();
  }
}
