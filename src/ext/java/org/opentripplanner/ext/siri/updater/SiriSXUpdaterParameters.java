package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record SiriSXUpdaterParameters(
  String configRef,
  String feedId,
  String url,
  String requestorRef,
  int frequencySec,
  int earlyStartSec,
  int timeoutSec,
  boolean blockReadinessUntilInitialized,
  HttpHeaders requestHeaders
)
  implements PollingGraphUpdaterParameters {}
