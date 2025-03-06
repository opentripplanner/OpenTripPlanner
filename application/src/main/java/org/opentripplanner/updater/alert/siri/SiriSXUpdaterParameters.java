package org.opentripplanner.updater.alert.siri;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;

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
  implements SiriSXUpdater.Parameters {}
