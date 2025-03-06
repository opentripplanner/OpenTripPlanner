package org.opentripplanner.updater.alert.siri.mapping;

import org.opentripplanner.routing.alertpatch.AlertSeverity;
import uk.org.siri.siri20.SeverityEnumeration;

/**
 * Util class for mapping SIRI's severity enums into internal {@link AlertSeverity}.
 */
public class SiriSeverityMapper {

  /**
   * Returns internal {@link AlertSeverity} enum counterpart for SIRI enum. Defaults to returning
   * WARNING.
   */
  public static AlertSeverity getAlertSeverityForSiriSeverity(SeverityEnumeration severity) {
    if (severity == null) {
      return AlertSeverity.WARNING;
    }
    return switch (severity) {
      case PTI_26_255, UNDEFINED -> AlertSeverity.UNDEFINED;
      case PTI_26_0, UNKNOWN -> AlertSeverity.UNKNOWN_SEVERITY;
      case PTI_26_6, NO_IMPACT -> AlertSeverity.INFO;
      case PTI_26_1, VERY_SLIGHT -> AlertSeverity.VERY_SLIGHT;
      case PTI_26_2, SLIGHT -> AlertSeverity.SLIGHT;
      case PTI_26_4, SEVERE -> AlertSeverity.SEVERE;
      case PTI_26_5, VERY_SEVERE -> AlertSeverity.VERY_SEVERE;
      case PTI_26_3, NORMAL -> AlertSeverity.WARNING;
    };
  }
}
