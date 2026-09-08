package org.opentripplanner.apis.gtfs.mapping;

import java.util.List;
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
      case UNKNOWN_SEVERITY -> GraphQLAlertSeverityLevelType.UNKNOWN_SEVERITY;
    };
  }

  /**
   * Returns the internal {@link AlertSeverity} values that map to a GraphQL API severity level.
   * Since multiple internal severities map to a single GraphQL severity, a list is returned.
   */
  public static List<AlertSeverity> getAlertSeverities(GraphQLAlertSeverityLevelType severity) {
    return switch (severity) {
      case INFO -> List.of(AlertSeverity.INFO);
      case WARNING -> List.of(
        AlertSeverity.VERY_SLIGHT,
        AlertSeverity.SLIGHT,
        AlertSeverity.WARNING
      );
      case SEVERE -> List.of(AlertSeverity.VERY_SEVERE, AlertSeverity.SEVERE);
      case UNKNOWN_SEVERITY -> List.of(AlertSeverity.UNKNOWN_SEVERITY);
    };
  }
}
