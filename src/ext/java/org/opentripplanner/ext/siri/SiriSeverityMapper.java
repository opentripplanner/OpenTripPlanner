package org.opentripplanner.ext.siri;

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
        switch (severity) {
            case PTI_26_255:
            case UNDEFINED:
                return AlertSeverity.UNDEFINED;
            case PTI_26_0:
            case UNKNOWN:
                return AlertSeverity.UNKNOWN_SEVERITY;
            case PTI_26_6:
            case NO_IMPACT:
                return AlertSeverity.INFO;
            case PTI_26_1:
            case VERY_SLIGHT:
                return AlertSeverity.VERY_SLIGHT;
            case PTI_26_2:
            case SLIGHT:
                return AlertSeverity.SLIGHT;
            case PTI_26_4:
            case SEVERE:
                return AlertSeverity.SEVERE;
            case PTI_26_5:
            case VERY_SEVERE:
                return AlertSeverity.VERY_SEVERE;
            case PTI_26_3:
            case NORMAL:
            default: {
                return AlertSeverity.WARNING;
            }
        }
    }
}
