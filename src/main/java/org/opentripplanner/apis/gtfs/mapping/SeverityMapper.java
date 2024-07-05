package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertSeverityLevelType;
import org.opentripplanner.routing.alertpatch.AlertSeverity;

/**
 * Class for mapping {@link AlertSeverity} to GraphQL API severity (GTFS RT).
 */
public class SeverityMapper {

  /**
   * Returns GraphQL API counterpart for internal {@link AlertSeverity} enum. Defaults
   * to returning UNKNOWN_SEVERITY.
   */
  public static GraphQLAlertSeverityLevelType getGraphQLSeverity(AlertSeverity severity) {
    if (severity == null) {
      return GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
    }
    return switch (severity) {
      case INFO -> GraphQLAlertSeverityLevelType.INFO;
      case VERY_SLIGHT, SLIGHT, WARNING -> GraphQLAlertSeverityLevelType.WARNING;
      case VERY_SEVERE, SEVERE -> GraphQLAlertSeverityLevelType.SEVERE;
      case UNDEFINED, UNKNOWN_SEVERITY -> GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
    };
  }
}
