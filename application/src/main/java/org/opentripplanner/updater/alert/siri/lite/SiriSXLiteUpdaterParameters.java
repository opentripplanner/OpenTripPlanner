package org.opentripplanner.updater.alert.siri.lite;

import java.net.URI;
import java.time.Duration;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.alert.siri.SiriSXUpdater;

public record SiriSXLiteUpdaterParameters(
  String configRef,
  String feedId,
  URI uri,
  Duration frequency,
  Duration earlyStart,
  Duration timeout,
  HttpHeaders requestHeaders
) implements SiriSXUpdater.Parameters {
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
