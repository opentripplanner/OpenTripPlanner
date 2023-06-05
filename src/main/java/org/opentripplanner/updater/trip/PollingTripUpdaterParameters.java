package org.opentripplanner.updater.trip;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record PollingTripUpdaterParameters(
  String configRef,
  Duration frequency,
  boolean fuzzyTripMatching,
  BackwardsDelayPropagationType backwardsDelayPropagationType,

  String feedId,
  String url,
  HttpHeaders headers
)
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {}
