package org.opentripplanner.service.vehiclerental.street;

import java.util.List;
import java.util.Set;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.search.state.State;

/**
 * Marks an edge as crossing a geofencing zone boundary.
 * This extension does NOT itself ban traversal or drop-off - instead, it signals
 * that the routing state should be updated to reflect entering or exiting the zone.
 * The actual restrictions are enforced through state-based zone tracking.
 *
 * <p>For boundary-only geofencing:
 * <ul>
 *   <li>At vehicle pickup: spatial query determines initial zones</li>
 *   <li>At boundary crossing: state.currentGeofencingZones is updated</li>
 *   <li>At drop-off/traversal check: state zones are checked for bans</li>
 * </ul>
 */
public record GeofencingBoundaryExtension(GeofencingZone zone, boolean entering) implements
  RentalRestrictionExtension {
  /**
   * The boundary itself does not ban traversal.
   * Traversal restrictions are enforced by checking state.currentGeofencingZones.
   */
  @Override
  public boolean traversalBanned(State state) {
    return false;
  }

  /**
   * The boundary itself does not ban drop-off.
   * Drop-off restrictions are enforced by checking state.currentGeofencingZones.
   */
  @Override
  public boolean dropOffBanned(State state) {
    return false;
  }

  @Override
  public boolean appliesTo(State state) {
    if (!state.isRentingVehicle()) {
      return false;
    }
    return (
      state.unknownRentalNetwork() || zone.id().getFeedId().equals(state.getVehicleRentalNetwork())
    );
  }

  @Override
  public int priority() {
    return zone.priority();
  }

  @Override
  public Set<RestrictionType> debugTypes() {
    return Set.of(entering ? RestrictionType.ZONE_ENTRY : RestrictionType.ZONE_EXIT);
  }

  @Override
  public List<RentalRestrictionExtension> toList() {
    return List.of(this);
  }

  @Override
  public Set<String> noDropOffNetworks() {
    return Set.of();
  }

  @Override
  public List<String> networks() {
    return List.of(zone.id().getFeedId());
  }

  @Override
  public boolean hasRestrictions() {
    // Boundaries don't themselves have restrictions - they modify state
    return false;
  }

  @Override
  public boolean hasGeofencingBoundary() {
    return true;
  }

  @Override
  public String toString() {
    return (entering ? "ENTER:" : "EXIT:") + zone.id().toString();
  }
}
