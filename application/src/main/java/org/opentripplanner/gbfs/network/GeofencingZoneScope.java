package org.opentripplanner.gbfs.network;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/**
 * The scope of a network's geofencing zones: which kind of change to the street graph they are, and
 * therefore which phase computes and applies them. Configured per network as
 * {@code applyGeofencingZones}, which names the setting from the reader's side - when the zones are
 * applied - where this type names it from the graph's.
 * <p>
 * The vocabulary mirrors {@link org.opentripplanner.street.Scope}, which OTP already defines for
 * this distinction: {@code PERMANENT} changes to the street graph are done during graph building,
 * {@code REALTIME} changes are done by updaters. This is a separate enum rather than {@code Scope}
 * itself because {@code Scope.REQUEST} is meaningless here.
 * <p>
 * The two phases are mutually exclusive by construction, so a network cannot have its zones
 * applied twice.
 */
public enum GeofencingZoneScope implements DocumentedEnum<GeofencingZoneScope> {
  PERMANENT,
  REALTIME,
  OFF;

  @Override
  public String typeDescription() {
    return "When this network's geofencing zones are computed and applied.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case PERMANENT -> "The vehicle rental geofencing graph builder loads and applies the zones.";
      case REALTIME -> "The vehicle rental updater loads and applies the zones.";
      case OFF -> """
      The zones are not processed for this network. Use this to opt a single network out of a
      `defaults` block that enables them.""";
    };
  }
}
