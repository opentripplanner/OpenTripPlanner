package org.opentripplanner.updater.trip.gtfs.updater.http;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.updater.trip.gtfs.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.ForwardsDelayPropagationType;

public record PollingTripUpdaterParameters(
  String configRef,
  Duration frequency,
  boolean fuzzyTripMatching,
  ForwardsDelayPropagationType forwardsDelayPropagationType,
  BackwardsDelayPropagationType backwardsDelayPropagationType,

  String feedId,
  String url,
  HttpHeaders headers
)
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {}
