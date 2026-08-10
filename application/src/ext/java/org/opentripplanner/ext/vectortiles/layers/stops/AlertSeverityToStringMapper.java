package org.opentripplanner.ext.vectortiles.layers.stops;

import org.opentripplanner.routing.alertpatch.AlertSeverity;

final class AlertSeverityToStringMapper {

  private AlertSeverityToStringMapper() {}

  static String map(AlertSeverity severity) {
    return switch (severity) {
      case INFO -> "INFO";
      case WARNING -> "WARNING";
      case SEVERE -> "SEVERE";
      default -> "UNKNOWN_SEVERITY";
    };
  }
}
