package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.alertpatch.AlertSeverity;

/**
 * Class for mapping {@link AlertSeverity} to GraphQL API severity (GTFS RT).
 */
public class SeverityMapper {

  /**
   * Returns GraphQL API string counter part for internal {@link AlertSeverity} enum. Defaults
   * to returning UNKNOWN_SEVERITY.
   */
  public static GraphQLTypes.GraphQLAlertSeverityLevelType getGraphQLSeverity(
    AlertSeverity severity
  ) {
    if (severity == null) {
      return GraphQLTypes.GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
    }
    return switch (severity) {
      case INFO -> GraphQLTypes.GraphQLAlertSeverityLevelType.INFO;
      case VERY_SLIGHT, SLIGHT, WARNING -> GraphQLTypes.GraphQLAlertSeverityLevelType.WARNING;
      case VERY_SEVERE, SEVERE -> GraphQLTypes.GraphQLAlertSeverityLevelType.SEVERE;
      case UNDEFINED,
        UNKNOWN_SEVERITY -> GraphQLTypes.GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
    };
  }
}
