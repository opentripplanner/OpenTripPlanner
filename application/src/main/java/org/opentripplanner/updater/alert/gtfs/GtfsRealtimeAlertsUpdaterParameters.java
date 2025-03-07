package org.opentripplanner.updater.alert.gtfs;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record GtfsRealtimeAlertsUpdaterParameters(
  String configRef,
  String feedId,
  String url,
  int earlyStartSec,
  boolean fuzzyTripMatching,
  Duration frequency,
  HttpHeaders headers
)
  implements PollingGraphUpdaterParameters {}
