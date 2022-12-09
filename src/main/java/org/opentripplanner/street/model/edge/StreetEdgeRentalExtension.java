package org.opentripplanner.street.model.edge;

import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

public interface StreetEdgeRentalExtension {
  boolean traversalBanned(State state);

  boolean dropOffBanned(State state);

  String network();

  class GeofencingZoneExtension implements StreetEdgeRentalExtension {

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
    public String network() {
      return zone.id().getFeedId();
    }

    @Override
    public String toString() {
      return zone.id().toString();
    }
  }
}
