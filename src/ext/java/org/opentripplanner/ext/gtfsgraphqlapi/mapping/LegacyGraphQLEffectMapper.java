package org.opentripplanner.ext.gtfsgraphqlapi.mapping;

import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLAlertEffectType;
import org.opentripplanner.routing.alertpatch.AlertEffect;

/**
 * Class for mapping {@link AlertEffect} to LegacyGraphQL API effect (GTFS RT).
 */
public class LegacyGraphQLEffectMapper {

  /**
   * Returns LegacyGraphQL API string counter part for internal {@link AlertEffect} enum. Defaults
   * to returning UNKNOWN_Effect.
   */
  public static LegacyGraphQLAlertEffectType getLegacyGraphQLEffect(AlertEffect effect) {
    if (effect == null) {
      return LegacyGraphQLAlertEffectType.UNKNOWN_EFFECT;
    }
    return switch (effect) {
      case NO_SERVICE -> LegacyGraphQLAlertEffectType.NO_SERVICE;
      case REDUCED_SERVICE -> LegacyGraphQLAlertEffectType.REDUCED_SERVICE;
      case SIGNIFICANT_DELAYS -> LegacyGraphQLAlertEffectType.SIGNIFICANT_DELAYS;
      case DETOUR -> LegacyGraphQLAlertEffectType.DETOUR;
      case ADDITIONAL_SERVICE -> LegacyGraphQLAlertEffectType.ADDITIONAL_SERVICE;
      case MODIFIED_SERVICE -> LegacyGraphQLAlertEffectType.MODIFIED_SERVICE;
      case OTHER_EFFECT -> LegacyGraphQLAlertEffectType.OTHER_EFFECT;
      case UNKNOWN_EFFECT -> LegacyGraphQLAlertEffectType.UNKNOWN_EFFECT;
      case STOP_MOVED -> LegacyGraphQLAlertEffectType.STOP_MOVED;
      case NO_EFFECT -> LegacyGraphQLAlertEffectType.NO_EFFECT;
      case ACCESSIBILITY_ISSUE -> LegacyGraphQLAlertEffectType.ACCESSIBILITY_ISSUE;
    };
  }
}
