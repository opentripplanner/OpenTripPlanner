package org.opentripplanner.updater.trip;

import java.util.Map;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record PollingTripUpdaterParameters(
  String configRef,
  int frequencySec,
  boolean fuzzyTripMatching,
  BackwardsDelayPropagationType backwardsDelayPropagationType,

  String feedId,
  String url,
  HttpHeaders headers
)
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {}
