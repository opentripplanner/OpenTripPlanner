package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertCauseType;
import org.opentripplanner.routing.alertpatch.AlertCause;

/**
 * Class for mapping {@link AlertCause} to GraphQL API cause (GTFS RT).
 */
public class AlertCauseMapper {

  /**
   * Returns GraphQL API string counter part for internal {@link AlertCause} enum. Defaults to
   * returning UNKNOWN_CAUSE.
   */
  public static GraphQLAlertCauseType getGraphQLCause(AlertCause cause) {
    if (cause == null) {
      return GraphQLAlertCauseType.UNKNOWN_CAUSE;
    }
    return switch (cause) {
      case UNKNOWN_CAUSE -> GraphQLAlertCauseType.UNKNOWN_CAUSE;
      case OTHER_CAUSE -> GraphQLAlertCauseType.OTHER_CAUSE;
      case TECHNICAL_PROBLEM -> GraphQLAlertCauseType.TECHNICAL_PROBLEM;
      case STRIKE -> GraphQLAlertCauseType.STRIKE;
      case DEMONSTRATION -> GraphQLAlertCauseType.DEMONSTRATION;
      case ACCIDENT -> GraphQLAlertCauseType.ACCIDENT;
      case HOLIDAY -> GraphQLAlertCauseType.HOLIDAY;
      case WEATHER -> GraphQLAlertCauseType.WEATHER;
      case MAINTENANCE -> GraphQLAlertCauseType.MAINTENANCE;
      case CONSTRUCTION -> GraphQLAlertCauseType.CONSTRUCTION;
      case POLICE_ACTIVITY -> GraphQLAlertCauseType.POLICE_ACTIVITY;
      case MEDICAL_EMERGENCY -> GraphQLAlertCauseType.MEDICAL_EMERGENCY;
    };
  }
}
