package org.opentripplanner.updater.alert;

import java.util.Map;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;

public record GtfsRealtimeAlertsUpdaterParameters(
  String configRef,
  String feedId,
  String url,
  int earlyStartSec,
  boolean fuzzyTripMatching,
  int frequencySec,
  HttpHeaders headers
)
  implements PollingGraphUpdaterParameters {}
