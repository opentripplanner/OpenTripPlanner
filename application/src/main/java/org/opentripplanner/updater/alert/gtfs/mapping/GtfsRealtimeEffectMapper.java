package org.opentripplanner.updater.alert.gtfs.mapping;

import com.google.transit.realtime.GtfsRealtime.Alert.Effect;
import org.opentripplanner.routing.alertpatch.AlertEffect;

/**
 * Util class for mapping GTFS realtime effect enums into internal {@link AlertEffect}.
 */
public class GtfsRealtimeEffectMapper {

  /**
   * Returns internal {@link AlertEffect} enum counterpart for GTFS realtime enum. Defaults to
   * returning UNKNOWN_EFFECT.
   */
  public static AlertEffect getAlertEffectForGtfsRtEffect(Effect effect) {
    if (effect == null) {
      return AlertEffect.UNKNOWN_EFFECT;
    }
    switch (effect) {
      case NO_SERVICE:
        return AlertEffect.NO_SERVICE;
      case REDUCED_SERVICE:
        return AlertEffect.REDUCED_SERVICE;
      case SIGNIFICANT_DELAYS:
        return AlertEffect.SIGNIFICANT_DELAYS;
      case DETOUR:
        return AlertEffect.DETOUR;
      case ADDITIONAL_SERVICE:
        return AlertEffect.ADDITIONAL_SERVICE;
      case MODIFIED_SERVICE:
        return AlertEffect.MODIFIED_SERVICE;
      case OTHER_EFFECT:
        return AlertEffect.OTHER_EFFECT;
      case STOP_MOVED:
        return AlertEffect.STOP_MOVED;
      case NO_EFFECT:
        return AlertEffect.NO_EFFECT;
      case ACCESSIBILITY_ISSUE:
        return AlertEffect.ACCESSIBILITY_ISSUE;
      case UNKNOWN_EFFECT:
      default: {
        return AlertEffect.UNKNOWN_EFFECT;
      }
    }
  }
}
