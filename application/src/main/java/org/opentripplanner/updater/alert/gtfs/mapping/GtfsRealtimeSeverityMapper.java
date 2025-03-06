package org.opentripplanner.updater.alert.gtfs.mapping;

import com.google.transit.realtime.GtfsRealtime.Alert.SeverityLevel;
import org.opentripplanner.routing.alertpatch.AlertSeverity;

/**
 * Util class for mapping GTFS realtime severity enums into internal {@link AlertSeverity}.
 */
public class GtfsRealtimeSeverityMapper {

  /**
   * Returns internal {@link AlertSeverity} enum counterpart for GTFS realtime enum. Defaults to
   * returning UNKNOWN_SEVERITY.
   */
  public static AlertSeverity getAlertSeverityForGtfsRtSeverity(SeverityLevel severity) {
    if (severity == null) {
      return AlertSeverity.UNKNOWN_SEVERITY;
    }
    switch (severity) {
      case INFO:
        return AlertSeverity.INFO;
      case WARNING:
        return AlertSeverity.WARNING;
      case SEVERE:
        return AlertSeverity.SEVERE;
      case UNKNOWN_SEVERITY:
      default: {
        return AlertSeverity.UNKNOWN_SEVERITY;
      }
    }
  }
}
