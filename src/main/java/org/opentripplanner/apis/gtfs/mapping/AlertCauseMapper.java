package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.alertpatch.AlertCause;

/**
 * Class for mapping {@link AlertCause} to GraphQL API cause (GTFS RT).
 */
public class AlertCauseMapper {

  /**
   * Returns GraphQL API string counter part for internal {@link AlertCause} enum. Defaults to
   * returning UNKNOWN_CAUSE.
   */
  public static GraphQLTypes.GraphQLAlertCauseType getGraphQLCause(AlertCause cause) {
    if (cause == null) {
      return GraphQLTypes.GraphQLAlertCauseType.UNKNOWN_CAUSE;
    }
    return switch (cause) {
      case UNKNOWN_CAUSE -> GraphQLTypes.GraphQLAlertCauseType.UNKNOWN_CAUSE;
      case OTHER_CAUSE -> GraphQLTypes.GraphQLAlertCauseType.OTHER_CAUSE;
      case TECHNICAL_PROBLEM -> GraphQLTypes.GraphQLAlertCauseType.TECHNICAL_PROBLEM;
      case STRIKE -> GraphQLTypes.GraphQLAlertCauseType.STRIKE;
      case DEMONSTRATION -> GraphQLTypes.GraphQLAlertCauseType.DEMONSTRATION;
      case ACCIDENT -> GraphQLTypes.GraphQLAlertCauseType.ACCIDENT;
      case HOLIDAY -> GraphQLTypes.GraphQLAlertCauseType.HOLIDAY;
      case WEATHER -> GraphQLTypes.GraphQLAlertCauseType.WEATHER;
      case MAINTENANCE -> GraphQLTypes.GraphQLAlertCauseType.MAINTENANCE;
      case CONSTRUCTION -> GraphQLTypes.GraphQLAlertCauseType.CONSTRUCTION;
      case POLICE_ACTIVITY -> GraphQLTypes.GraphQLAlertCauseType.POLICE_ACTIVITY;
      case MEDICAL_EMERGENCY -> GraphQLTypes.GraphQLAlertCauseType.MEDICAL_EMERGENCY;
    };
  }
}
