package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.alertpatch.AlertEffect;

/**
 * Class for mapping {@link AlertEffect} to GraphQL API effect (GTFS RT).
 */
public class AlertEffectMapper {

  /**
   * Returns GraphQL API string counter part for internal {@link AlertEffect} enum. Defaults
   * to returning UNKNOWN_Effect.
   */
  public static GraphQLTypes.GraphQLAlertEffectType getGraphQLEffect(AlertEffect effect) {
    if (effect == null) {
      return GraphQLTypes.GraphQLAlertEffectType.UNKNOWN_EFFECT;
    }
    return switch (effect) {
      case NO_SERVICE -> GraphQLTypes.GraphQLAlertEffectType.NO_SERVICE;
      case REDUCED_SERVICE -> GraphQLTypes.GraphQLAlertEffectType.REDUCED_SERVICE;
      case SIGNIFICANT_DELAYS -> GraphQLTypes.GraphQLAlertEffectType.SIGNIFICANT_DELAYS;
      case DETOUR -> GraphQLTypes.GraphQLAlertEffectType.DETOUR;
      case ADDITIONAL_SERVICE -> GraphQLTypes.GraphQLAlertEffectType.ADDITIONAL_SERVICE;
      case MODIFIED_SERVICE -> GraphQLTypes.GraphQLAlertEffectType.MODIFIED_SERVICE;
      case OTHER_EFFECT -> GraphQLTypes.GraphQLAlertEffectType.OTHER_EFFECT;
      case UNKNOWN_EFFECT -> GraphQLTypes.GraphQLAlertEffectType.UNKNOWN_EFFECT;
      case STOP_MOVED -> GraphQLTypes.GraphQLAlertEffectType.STOP_MOVED;
      case NO_EFFECT -> GraphQLTypes.GraphQLAlertEffectType.NO_EFFECT;
      case ACCESSIBILITY_ISSUE -> GraphQLTypes.GraphQLAlertEffectType.ACCESSIBILITY_ISSUE;
    };
  }
}
