package org.opentripplanner.street.model.edge;

import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

public interface StreetEdgeTraversalExtension {
  boolean traversalBanned(State state);

  boolean dropOffBanned(State state);

  class GeofencingZoneExtension implements StreetEdgeTraversalExtension {

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
          state.getVehicleRentalNetwork().equals(zone.id().getFeedId()) &&
          zone.passingThroughBanned()
        );
      } else {
        return false;
      }
    }

    @Override
    public boolean dropOffBanned(State state) {
      if (state.isRentingVehicle()) {
        return (
          state.getVehicleRentalNetwork().equals(zone.id().getFeedId()) && zone.dropOffBanned()
        );
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return zone.id().toString();
    }
  }
}
