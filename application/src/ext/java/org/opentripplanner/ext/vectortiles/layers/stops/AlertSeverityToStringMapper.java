package org.opentripplanner.ext.vectortiles.layers.stops;

import org.opentripplanner.routing.alertpatch.AlertSeverity;

final class AlertSeverityToStringMapper {

  private AlertSeverityToStringMapper() {}

  static String map(AlertSeverity severity) {
    return switch (severity) {
      case INFO -> "INFO";
      case VERY_SLIGHT, SLIGHT, WARNING -> "WARNING";
      case VERY_SEVERE, SEVERE -> "SEVERE";
      case UNKNOWN_SEVERITY -> "UNKNOWN_SEVERITY";
    };
  }
}
