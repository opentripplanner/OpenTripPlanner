package org.opentripplanner.gbfs.network;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/**
 * The OTP phase that computes and applies a network's geofencing zones. Configured per network as
 * {@code applyGeofencingZones}.
 * <p>
 * {@code GRAPH_BUILD} corresponds to {@link org.opentripplanner.street.Scope#PERMANENT} changes to
 * the street graph and {@code SERVE} to {@link org.opentripplanner.street.Scope#REALTIME} ones.
 * This is a separate enum rather than {@code Scope} itself because it names the phase a deployment
 * chooses rather than the kind of graph change that results, and because {@code Scope.REQUEST} is
 * meaningless here.
 * <p>
 * The two phases are mutually exclusive by construction, so a network cannot have its zones
 * applied twice.
 */
public enum GeofencingZoneOtpPhase implements DocumentedEnum<GeofencingZoneOtpPhase> {
  GRAPH_BUILD,
  SERVE,
  OFF;

  @Override
  public String typeDescription() {
    return "When this network's geofencing zones are computed and applied.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case GRAPH_BUILD -> "The vehicle rental geofencing graph builder loads and applies the zones.";
      case SERVE -> "The vehicle rental updater loads and applies the zones.";
      case OFF -> """
      The zones are not processed for this network. Use this to opt a single network out of a
      `defaults` block that enables them.""";
    };
  }
}
