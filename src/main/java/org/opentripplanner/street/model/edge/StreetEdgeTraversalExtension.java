package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.search.state.State;

public interface StreetEdgeTraversalExtension {
  boolean isBanned(State state);

  class BanRentalNetwork implements StreetEdgeTraversalExtension {

    private final String bannedNetwork;

    public BanRentalNetwork(String bannedNetwork) {
      this.bannedNetwork = bannedNetwork;
    }

    @Override
    public boolean isBanned(State state) {
      return state.isRentingVehicle() && state.getVehicleRentalNetwork().equals(bannedNetwork);
    }
  };
}
