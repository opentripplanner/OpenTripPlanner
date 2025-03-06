package org.opentripplanner.updater.alert.gtfs.mapping;

import com.google.transit.realtime.GtfsRealtime.Alert.Cause;
import org.opentripplanner.routing.alertpatch.AlertCause;

/**
 * Util class for mapping GTFS realtime cause enums into internal {@link AlertCause}.
 */
public class GtfsRealtimeCauseMapper {

  /**
   * Returns internal {@link AlertCause} enum counterpart for GTFS realtime enum. Defaults to
   * returning UNKNOWN_CAUSE.
   */
  public static AlertCause getAlertCauseForGtfsRtCause(Cause cause) {
    if (cause == null) {
      return AlertCause.UNKNOWN_CAUSE;
    }
    switch (cause) {
      case OTHER_CAUSE:
        return AlertCause.OTHER_CAUSE;
      case TECHNICAL_PROBLEM:
        return AlertCause.TECHNICAL_PROBLEM;
      case STRIKE:
        return AlertCause.STRIKE;
      case DEMONSTRATION:
        return AlertCause.DEMONSTRATION;
      case ACCIDENT:
        return AlertCause.ACCIDENT;
      case HOLIDAY:
        return AlertCause.HOLIDAY;
      case WEATHER:
        return AlertCause.WEATHER;
      case MAINTENANCE:
        return AlertCause.MAINTENANCE;
      case CONSTRUCTION:
        return AlertCause.CONSTRUCTION;
      case POLICE_ACTIVITY:
        return AlertCause.POLICE_ACTIVITY;
      case MEDICAL_EMERGENCY:
        return AlertCause.MEDICAL_EMERGENCY;
      case UNKNOWN_CAUSE:
      default: {
        return AlertCause.UNKNOWN_CAUSE;
      }
    }
  }
}
