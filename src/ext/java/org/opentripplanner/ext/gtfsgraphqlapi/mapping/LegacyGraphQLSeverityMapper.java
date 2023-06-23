package org.opentripplanner.ext.gtfsgraphqlapi.mapping;

import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLAlertSeverityLevelType;
import org.opentripplanner.routing.alertpatch.AlertSeverity;

/**
 * Class for mapping {@link AlertSeverity} to LegacyGraphQL API severity (GTFS RT).
 */
public class LegacyGraphQLSeverityMapper {

  /**
   * Returns LegacyGraphQL API string counter part for internal {@link AlertSeverity} enum. Defaults
   * to returning UNKNOWN_SEVERITY.
   */
  public static LegacyGraphQLAlertSeverityLevelType getLegacyGraphQLSeverity(
    AlertSeverity severity
  ) {
    if (severity == null) {
      return LegacyGraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
    }
    return switch (severity) {
      case INFO -> LegacyGraphQLAlertSeverityLevelType.INFO;
      case VERY_SLIGHT, SLIGHT, WARNING -> LegacyGraphQLAlertSeverityLevelType.WARNING;
      case VERY_SEVERE, SEVERE -> LegacyGraphQLAlertSeverityLevelType.SEVERE;
      case UNDEFINED, UNKNOWN_SEVERITY -> LegacyGraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
    };
  }
}
