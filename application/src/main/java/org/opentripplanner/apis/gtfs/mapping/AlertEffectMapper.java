package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAlertEffectType;
import org.opentripplanner.routing.alertpatch.AlertEffect;

/**
 * Class for mapping {@link AlertEffect} to GraphQL API effect (GTFS RT).
 */
public class AlertEffectMapper {

  /**
   * Returns GraphQL API string counter part for internal {@link AlertEffect} enum. Defaults
   * to returning UNKNOWN_Effect.
   */
  public static GraphQLAlertEffectType getGraphQLEffect(AlertEffect effect) {
    if (effect == null) {
      return GraphQLAlertEffectType.UNKNOWN_EFFECT;
    }
    return switch (effect) {
      case NO_SERVICE -> GraphQLAlertEffectType.NO_SERVICE;
      case REDUCED_SERVICE -> GraphQLAlertEffectType.REDUCED_SERVICE;
      case SIGNIFICANT_DELAYS -> GraphQLAlertEffectType.SIGNIFICANT_DELAYS;
      case DETOUR -> GraphQLAlertEffectType.DETOUR;
      case ADDITIONAL_SERVICE -> GraphQLAlertEffectType.ADDITIONAL_SERVICE;
      case MODIFIED_SERVICE -> GraphQLAlertEffectType.MODIFIED_SERVICE;
      case OTHER_EFFECT -> GraphQLAlertEffectType.OTHER_EFFECT;
      case UNKNOWN_EFFECT -> GraphQLAlertEffectType.UNKNOWN_EFFECT;
      case STOP_MOVED -> GraphQLAlertEffectType.STOP_MOVED;
      case NO_EFFECT -> GraphQLAlertEffectType.NO_EFFECT;
      case ACCESSIBILITY_ISSUE -> GraphQLAlertEffectType.ACCESSIBILITY_ISSUE;
    };
  }
}
