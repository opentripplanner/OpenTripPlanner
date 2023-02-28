package org.opentripplanner.service.vehiclerental.street;

import java.util.EnumSet;
import java.util.List;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.search.state.State;

/**
 * Traversal is restricted by the properties of the geofencing zone.
 */
public final class GeofencingZoneExtension implements RentalRestrictionExtension {

  private final GeofencingZone zone;

  public GeofencingZoneExtension(GeofencingZone zone) {
    this.zone = zone;
  }

  public GeofencingZone zone() {
    return zone;
  }

  @Override
  public boolean traversalBanned(State state) {
    if (state.isRentingVehicle()) {
      return (
        zone.traversalBanned() && zone.id().getFeedId().equals(state.getVehicleRentalNetwork())
      );
    } else {
      return false;
    }
  }

  @Override
  public boolean dropOffBanned(State state) {
    if (state.isRentingVehicle()) {
      return (
        zone.dropOffBanned() && zone.id().getFeedId().equals(state.getVehicleRentalNetwork())
      );
    } else {
      return false;
    }
  }

  @Override
  public EnumSet<RestrictionType> debugTypes() {
    var set = EnumSet.noneOf(RestrictionType.class);
    if (zone.traversalBanned()) {
      set.add(RestrictionType.NO_TRAVERSAL);
    }
    if (zone.dropOffBanned()) {
      set.add(RestrictionType.NO_DROP_OFF);
    }
    return set;
  }

  @Override
  public List<RentalRestrictionExtension> toList() {
    return List.of(this);
  }

  @Override
  public List<String> networks() {
    return List.of(zone.id().getFeedId());
  }

  @Override
  public String toString() {
    return zone.id().toString();
  }
}
