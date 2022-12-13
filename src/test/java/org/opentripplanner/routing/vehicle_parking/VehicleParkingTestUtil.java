package org.opentripplanner.routing.vehicle_parking;

import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleParkingTestUtil {

  private static final String TEST_FEED_ID = "TEST";

  public static VehicleParking createParkingWithEntrances(String id, double x, double y) {
    return createParkingWithEntrances(id, x, y, null);
  }

  public static VehicleParking createParkingWithEntrances(
    String id,
    double x,
    double y,
    VehicleParkingSpaces vehiclePlaces
  ) {
    VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
      builder
        .entranceId(new FeedScopedId(TEST_FEED_ID, "Entrance " + id))
        .name(new NonLocalizedString("Entrance " + id))
        .coordinate(new WgsCoordinate(y, x))
        .walkAccessible(true);

    return VehicleParking
      .builder()
      .id(new FeedScopedId(TEST_FEED_ID, id))
      .bicyclePlaces(true)
      .capacity(vehiclePlaces)
      .availability(vehiclePlaces)
      .entrance(entrance)
      .build();
  }

  public static void createStreet(
    StreetVertex from,
    StreetVertex to,
    StreetTraversalPermission permissions
  ) {
    new StreetEdge(
      from,
      to,
      GeometryUtils.makeLineString(from.getLat(), from.getLon(), to.getLat(), to.getLon()),
      String.format("%s%s street", from.getDefaultName(), to.getDefaultName()),
      1,
      permissions,
      false
    );
  }
}
