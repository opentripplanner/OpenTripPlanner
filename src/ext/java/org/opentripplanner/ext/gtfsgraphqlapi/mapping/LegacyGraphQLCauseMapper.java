package org.opentripplanner.ext.gtfsgraphqlapi.mapping;

import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLAlertCauseType;
import org.opentripplanner.routing.alertpatch.AlertCause;

/**
 * Class for mapping {@link AlertCause} to LegacyGraphQL API cause (GTFS RT).
 */
public class LegacyGraphQLCauseMapper {

  /**
   * Returns LegacyGraphQL API string counter part for internal {@link AlertCause} enum. Defaults to
   * returning UNKNOWN_CAUSE.
   */
  public static LegacyGraphQLAlertCauseType getLegacyGraphQLCause(AlertCause cause) {
    if (cause == null) {
      return LegacyGraphQLAlertCauseType.UNKNOWN_CAUSE;
    }
    return switch (cause) {
      case UNKNOWN_CAUSE -> LegacyGraphQLAlertCauseType.UNKNOWN_CAUSE;
      case OTHER_CAUSE -> LegacyGraphQLAlertCauseType.OTHER_CAUSE;
      case TECHNICAL_PROBLEM -> LegacyGraphQLAlertCauseType.TECHNICAL_PROBLEM;
      case STRIKE -> LegacyGraphQLAlertCauseType.STRIKE;
      case DEMONSTRATION -> LegacyGraphQLAlertCauseType.DEMONSTRATION;
      case ACCIDENT -> LegacyGraphQLAlertCauseType.ACCIDENT;
      case HOLIDAY -> LegacyGraphQLAlertCauseType.HOLIDAY;
      case WEATHER -> LegacyGraphQLAlertCauseType.WEATHER;
      case MAINTENANCE -> LegacyGraphQLAlertCauseType.MAINTENANCE;
      case CONSTRUCTION -> LegacyGraphQLAlertCauseType.CONSTRUCTION;
      case POLICE_ACTIVITY -> LegacyGraphQLAlertCauseType.POLICE_ACTIVITY;
      case MEDICAL_EMERGENCY -> LegacyGraphQLAlertCauseType.MEDICAL_EMERGENCY;
    };
  }
}
