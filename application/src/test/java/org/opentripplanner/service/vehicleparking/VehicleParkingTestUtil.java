package org.opentripplanner.service.vehicleparking;

import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.street.model._data.StreetModelForTest;
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

    return StreetModelForTest.vehicleParking()
      .id(new FeedScopedId(TEST_FEED_ID, id))
      .bicyclePlaces(true)
      .capacity(vehiclePlaces)
      .availability(vehiclePlaces)
      .entrance(entrance)
      .build();
  }
}
