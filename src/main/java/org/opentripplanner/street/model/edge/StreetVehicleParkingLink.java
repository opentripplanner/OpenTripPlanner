package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * This represents the connection between a street vertex and a vehicle parking vertex.
 */
public class StreetVehicleParkingLink extends Edge {

  private final VehicleParkingEntranceVertex vehicleParkingEntranceVertex;

  public StreetVehicleParkingLink(StreetVertex fromv, VehicleParkingEntranceVertex tov) {
    super(fromv, tov);
    vehicleParkingEntranceVertex = tov;
  }

  public StreetVehicleParkingLink(VehicleParkingEntranceVertex fromv, StreetVertex tov) {
    super(fromv, tov);
    vehicleParkingEntranceVertex = fromv;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("fromv", fromv).addObj("tov", tov).toString();
  }

  @Override
  public State[] traverse(State s0) {
    // Disallow traversing two StreetBikeParkLinks in a row.
    // Prevents router using bike rental stations as shortcuts to get around
    // turn restrictions.
    if (s0.getBackEdge() instanceof StreetVehicleParkingLink) {
      return State.empty();
    }

    var entrance = vehicleParkingEntranceVertex.getParkingEntrance();
    if (s0.getNonTransitMode() == TraverseMode.CAR) {
      if (!entrance.isCarAccessible()) {
        return State.empty();
      }
    } else if (!entrance.isWalkAccessible()) {
      return State.empty();
    }

    var vehicleParking = vehicleParkingEntranceVertex.getVehicleParking();
    final VehicleParkingRequest parkingRequest = s0.getRequest().parking();
    if (traversalBanned(parkingRequest, vehicleParking)) {
      return State.empty();
    }

    StateEditor s1 = s0.edit(this);

    s1.incrementWeight(1);
    s1.setBackMode(null);
    return s1.makeStateArray();
  }

  private boolean traversalBanned(
    VehicleParkingRequest parkingRequest,
    VehicleParking vehicleParking
  ) {
    return !parkingRequest.filter().matches(vehicleParking);
  }

  @Override
  public I18NString getName() {
    return vehicleParkingEntranceVertex.getName();
  }

  public LineString getGeometry() {
    return null;
  }

  public double getDistanceMeters() {
    return 0;
  }
}
