package org.opentripplanner.updater.vehicle_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSVehicleTypesAvailable;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleEntityCounts;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleTypeCount;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.street.model.RentalFormFactor;

class GbfsStationStatusMapperTest {

  public static final RentalVehicleType TYPE_CAR = RentalVehicleType.of()
    .withFormFactor(RentalFormFactor.CAR)
    .withId(id("tc"))
    .build();
  public static final String ID = "s1";

  @Test
  void availableSpacesFromVehicles() {
    var gbfsStation = new GBFSStation();
    gbfsStation.setStationId(ID);
    gbfsStation.setNumDocksAvailable(3);
    gbfsStation.setNumBikesAvailable(4);
    var t = new GBFSVehicleTypesAvailable();
    t.setCount(1);
    t.setVehicleTypeId(TYPE_CAR.id().getId());

    gbfsStation.setVehicleTypesAvailable(List.of(t));

    var mapper = new GbfsStationStatusMapper(
      Map.of(ID, gbfsStation),
      Map.of(TYPE_CAR.id().getId(), TYPE_CAR)
    );

    var station = VehicleRentalStation.of().withId(id(ID)).build();

    var mapped = mapper.mapStationStatus(station);

    assertEquals(
      new RentalVehicleEntityCounts(3, List.of(new RentalVehicleTypeCount(TYPE_CAR, 3))),
      mapped.getVehicleSpaceCounts()
    );
  }
}
