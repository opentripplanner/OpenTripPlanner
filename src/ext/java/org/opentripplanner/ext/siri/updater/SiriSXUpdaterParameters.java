package org.opentripplanner.ext.siri.updater;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record SiriSXUpdaterParameters(
  String configRef,
  String feedId,
  String url,
  String requestorRef,
  Duration frequency,
  Duration earlyStart,
  Duration timeout,
  boolean blockReadinessUntilInitialized,
  HttpHeaders requestHeaders
)
  implements PollingGraphUpdaterParameters {}
