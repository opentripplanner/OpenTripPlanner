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

  /**
   * Returns the internal {@link AlertEffect} for a GraphQL API effect.
   */
  public static AlertEffect getAlertEffect(GraphQLAlertEffectType effect) {
    return switch (effect) {
      case NO_SERVICE -> AlertEffect.NO_SERVICE;
      case REDUCED_SERVICE -> AlertEffect.REDUCED_SERVICE;
      case SIGNIFICANT_DELAYS -> AlertEffect.SIGNIFICANT_DELAYS;
      case DETOUR -> AlertEffect.DETOUR;
      case ADDITIONAL_SERVICE -> AlertEffect.ADDITIONAL_SERVICE;
      case MODIFIED_SERVICE -> AlertEffect.MODIFIED_SERVICE;
      case OTHER_EFFECT -> AlertEffect.OTHER_EFFECT;
      case UNKNOWN_EFFECT -> AlertEffect.UNKNOWN_EFFECT;
      case STOP_MOVED -> AlertEffect.STOP_MOVED;
      case NO_EFFECT -> AlertEffect.NO_EFFECT;
      case ACCESSIBILITY_ISSUE -> AlertEffect.ACCESSIBILITY_ISSUE;
    };
  }
}
