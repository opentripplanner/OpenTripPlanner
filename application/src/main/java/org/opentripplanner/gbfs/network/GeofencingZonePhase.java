package org.opentripplanner.gbfs.network;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/**
 * Which phase computes and applies a network's geofencing zones.
 * <p>
 * The vocabulary mirrors {@link org.opentripplanner.street.Scope}, which OTP already defines for
 * this distinction: {@code REALTIME} changes to the street graph are done by updaters. This is a
 * separate enum rather than {@code Scope} itself because most of {@code Scope} is meaningless here.
 */
public enum GeofencingZonePhase implements DocumentedEnum<GeofencingZonePhase> {
  REALTIME,
  OFF;

  @Override
  public String typeDescription() {
    return "Which phase computes and applies this network's geofencing zones.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case REALTIME -> "The vehicle rental updater loads and applies the zones.";
      case OFF -> """
      The zones are not processed for this network. Use this to opt a single network out of a
      `defaults` block that enables them.""";
    };
  }
}
