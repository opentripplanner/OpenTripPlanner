package org.opentripplanner.ext.vectortiles.layers.stops;

import org.opentripplanner.routing.alertpatch.AlertEffect;

final class AlertEffectToStringMapper {

  private AlertEffectToStringMapper() {}

  static String map(AlertEffect effect) {
    return switch (effect) {
      case NO_SERVICE -> "NO_SERVICE";
      case REDUCED_SERVICE -> "REDUCED_SERVICE";
      case SIGNIFICANT_DELAYS -> "SIGNIFICANT_DELAYS";
      case DETOUR -> "DETOUR";
      case ADDITIONAL_SERVICE -> "ADDITIONAL_SERVICE";
      case MODIFIED_SERVICE -> "MODIFIED_SERVICE";
      case OTHER_EFFECT -> "OTHER_EFFECT";
      case UNKNOWN_EFFECT -> "UNKNOWN_EFFECT";
      case STOP_MOVED -> "STOP_MOVED";
      case NO_EFFECT -> "NO_EFFECT";
      case ACCESSIBILITY_ISSUE -> "ACCESSIBILITY_ISSUE";
    };
  }
}
