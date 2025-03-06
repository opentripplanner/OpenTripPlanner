package org.opentripplanner.apis.transmodel.mapping;

import org.opentripplanner.routing.alertpatch.AlertSeverity;

/**
 * Class for mapping {@link AlertSeverity} to transmodel severities.
 */
public class SeverityMapper {

  /**
   * Returns transmodel string counterpart for internal {@link AlertSeverity} enum. Defaults to
   * returning normal.
   */
  public static String getTransmodelSeverity(AlertSeverity severity) {
    if (severity == null) {
      return "normal";
    }
    switch (severity) {
      case UNDEFINED:
        return "undefined";
      case UNKNOWN_SEVERITY:
        return "unknown";
      case INFO:
        return "noImpact";
      case VERY_SLIGHT:
        return "verySlight";
      case SLIGHT:
        return "slight";
      case SEVERE:
        return "severe";
      case VERY_SEVERE:
        return "verySevere";
      case WARNING:
      default: {
        return "normal";
      }
    }
  }
}
