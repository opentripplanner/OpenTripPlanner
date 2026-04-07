package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.ParkingRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * This represents the connection between a street vertex and a vehicle parking vertex.
 */
public class StreetVehicleParkingLink extends Edge {

  private final VehicleParkingEntranceVertex vehicleParkingEntranceVertex;

  private StreetVehicleParkingLink(Vertex fromv, VehicleParkingEntranceVertex tov) {
    super(fromv, tov);
    vehicleParkingEntranceVertex = tov;
  }

  private StreetVehicleParkingLink(VehicleParkingEntranceVertex fromv, Vertex tov) {
    super(fromv, tov);
    vehicleParkingEntranceVertex = fromv;
  }

  /**
   * Either from or to needs to be a {@link VehicleParkingEntranceVertex}.
   */
  public static StreetVehicleParkingLink createStreetVehicleParkingLink(Vertex from, Vertex to) {
    if (from instanceof VehicleParkingEntranceVertex entrance) {
      return connectToGraph(new StreetVehicleParkingLink(entrance, to));
    }
    if (to instanceof VehicleParkingEntranceVertex entrance) {
      return connectToGraph(new StreetVehicleParkingLink(from, entrance));
    }
    throw new IllegalArgumentException(
      "One of the vertices needs to be an entrance vertex. Got: " +
        from.getClass() +
        " and " +
        to.getClass()
    );
  }

  public static StreetVehicleParkingLink createStreetVehicleParkingLink(
    VehicleParkingEntranceVertex fromv,
    StreetVertex tov
  ) {
    return connectToGraph(new StreetVehicleParkingLink(fromv, tov));
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
    if (s0.currentMode() == TraverseMode.CAR) {
      if (!entrance.isCarAccessible()) {
        return State.empty();
      }
    } else if (!entrance.isWalkAccessible()) {
      return State.empty();
    }

    var vehicleParking = vehicleParkingEntranceVertex.getVehicleParking();
    var parkingPreferences = s0.getRequest().parking(s0.currentMode());
    if (traversalBanned(parkingPreferences, vehicleParking)) {
      return State.empty();
    }

    StateEditor s1 = s0.edit(this);

    s1.incrementWeight(1);
    s1.setBackMode(null);
    return s1.makeStateArray();
  }

  private boolean traversalBanned(
    ParkingRequest parkingPreferences,
    VehicleParking vehicleParking
  ) {
    return !parkingPreferences.filter().matches(vehicleParking);
  }

  @Override
  public I18NString getName() {
    return vehicleParkingEntranceVertex.getName();
  }

  @Override
  public LineString getGeometry() {
    return GeometryUtils.makeLineString(fromv.getCoordinate(), tov.getCoordinate());
  }
}
