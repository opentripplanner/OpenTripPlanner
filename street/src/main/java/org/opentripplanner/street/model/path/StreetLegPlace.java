package org.opentripplanner.street.model.path;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;

/**
 * The start or end of a {@link StreetLeg}.
 *
 * @param vertex                 The vertex where the leg starts or ends
 * @param name                   A display name for the place. For intersections this is derived
 *                               from the names of the streets meeting there.
 * @param realTimeVehicleParking If the vertex is a vehicle parking entrance: whether real-time
 *                               availability data exists for the mode used. Always false otherwise.
 */
public record StreetLegPlace(Vertex vertex, I18NString name, boolean realTimeVehicleParking) {
  /**
   * Create the place for the vertex of the given state.
   */
  static StreetLegPlace of(State state) {
    Vertex vertex = state.getVertex();
    I18NString name = vertex.getName();

    // This gets nicer names instead of osm:node:id when changing mode of transport.
    // Names are generated from all the streets in a corner, same as names in origin and
    // destination. We use the name in TemporaryStreetLocation since this name generation already
    // happened when the temporary location was generated.
    if (
      vertex instanceof StreetVertex streetVertex && !(vertex instanceof TemporaryStreetLocation)
    ) {
      name = streetVertex.getIntersectionName();
    }

    boolean realTimeVehicleParking =
      vertex instanceof VehicleParkingEntranceVertex parkingVertex &&
      hasRealTimeVehicleParkingData(parkingVertex, state);

    return new StreetLegPlace(vertex, name, realTimeVehicleParking);
  }

  private static boolean hasRealTimeVehicleParkingData(
    VehicleParkingEntranceVertex vertex,
    State state
  ) {
    TraverseMode traverseMode = null;
    var request = state.getRequest();
    if (request.mode().includesDriving()) {
      traverseMode = TraverseMode.CAR;
    } else if (request.mode().includesBiking()) {
      traverseMode = TraverseMode.BICYCLE;
    }
    return vertex
      .getVehicleParking()
      .hasRealTimeDataForMode(traverseMode, request.wheelchairEnabled());
  }
}
