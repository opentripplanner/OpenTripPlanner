package org.opentripplanner.street.model.edge;

import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.search.state.State;

public sealed interface StreetEdgeRentalExtension {
  boolean traversalBanned(State state);

  boolean dropOffBanned(State state);

  String network();

  final class GeofencingZoneExtension implements StreetEdgeRentalExtension {

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
          zone.id().getFeedId().equals(state.getVehicleRentalNetwork()) &&
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
          zone.id().getFeedId().equals(state.getVehicleRentalNetwork()) && zone.dropOffBanned()
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

  final class BusinessAreaBorder implements StreetEdgeRentalExtension {

    private final String network;

    public BusinessAreaBorder(String network) {
      this.network = network;
    }

    @Override
    public boolean traversalBanned(State state) {
      return state.isRentingVehicle() && network.equals(state.getVehicleRentalNetwork());
    }

    @Override
    public boolean dropOffBanned(State state) {
      return traversalBanned(state);
    }

    @Override
    public String network() {
      return network;
    }
  }
}
