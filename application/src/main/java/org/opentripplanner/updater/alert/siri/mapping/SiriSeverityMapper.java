package org.opentripplanner.updater.alert.siri.mapping;

import org.opentripplanner.routing.alertpatch.AlertSeverity;
import uk.org.siri.siri21.SeverityEnumeration;

/**
 * Util class for mapping SIRI's severity enums into internal {@link AlertSeverity}.
 */
public class SiriSeverityMapper {

  /**
   * Returns internal {@link AlertSeverity} enum counterpart for SIRI enum. SIRI's {@code undefined}
   * severity is mapped to {@link AlertSeverity#WARNING}, which is also the default used when the
   * severity is missing or {@code normal}.
   */
  public static AlertSeverity getAlertSeverityForSiriSeverity(SeverityEnumeration severity) {
    if (severity == null) {
      return AlertSeverity.WARNING;
    }
    return switch (severity) {
      case UNDEFINED -> AlertSeverity.WARNING;
      case UNKNOWN -> AlertSeverity.UNKNOWN_SEVERITY;
      case NO_IMPACT -> AlertSeverity.INFO;
      case VERY_SLIGHT -> AlertSeverity.VERY_SLIGHT;
      case SLIGHT -> AlertSeverity.SLIGHT;
      case SEVERE -> AlertSeverity.SEVERE;
      case VERY_SEVERE -> AlertSeverity.VERY_SEVERE;
      case NORMAL -> AlertSeverity.WARNING;
    };
  }
}
