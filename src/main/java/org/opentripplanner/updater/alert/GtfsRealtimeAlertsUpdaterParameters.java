package org.opentripplanner.updater.alert;

import java.util.Map;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public record GtfsRealtimeAlertsUpdaterParameters(
  String configRef,
  String feedId,
  String url,
  int earlyStartSec,
  boolean fuzzyTripMatching,
  int frequencySec,
  Map<String, String> headers
)
  implements PollingGraphUpdaterParameters {
  public GtfsRealtimeAlertsUpdaterParameters {
    headers = Map.copyOf(headers);
  }
}
