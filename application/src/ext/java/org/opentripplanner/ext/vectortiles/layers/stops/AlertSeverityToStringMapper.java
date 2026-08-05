package org.opentripplanner.ext.vectortiles.layers.stops;

import org.opentripplanner.routing.alertpatch.AlertSeverity;

final class AlertSeverityToStringMapper {

  private AlertSeverityToStringMapper() {}

  static String map(AlertSeverity severity) {
    return switch (severity) {
      case UNDEFINED -> "UNKNOWN";
      case UNKNOWN_SEVERITY -> "UNKNOWN_SEVERITY";
      case INFO -> "INFO";
      case VERY_SLIGHT -> "VERY_SLIGHT";
      case SLIGHT -> "SLIGHT";
      case WARNING -> "WARNING";
      case SEVERE -> "SEVERE";
      case VERY_SEVERE -> "VERY_SEVERE";
    };
  }
}
