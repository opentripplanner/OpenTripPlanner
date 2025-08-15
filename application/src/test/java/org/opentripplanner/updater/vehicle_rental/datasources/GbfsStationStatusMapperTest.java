package org.opentripplanner.updater.vehicle_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSVehicleDocksAvailable;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSVehicleTypesAvailable;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleEntityCounts;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleTypeCount;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.street.model.RentalFormFactor;

class GbfsStationStatusMapperTest {

  private static final RentalVehicleType TYPE_CAR = RentalVehicleType.of()
    .withFormFactor(RentalFormFactor.CAR)
    .withId(id("tc"))
    .build();
  private static final String ID = "s1";
  private static final VehicleRentalStation STATION = VehicleRentalStation.of()
    .withId(id(ID))
    .build();

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

    var mapped = mapper.mapStationStatus(STATION);

    assertEquals(
      new RentalVehicleEntityCounts(3, List.of(new RentalVehicleTypeCount(TYPE_CAR, 3))),
      mapped.vehicleSpaceCounts()
    );

    assertEquals(Set.of(RentalFormFactor.CAR), mapped.formFactors());
  }

  @Test
  void availableSpacesFromTypes() {
    var gbfsStation = new GBFSStation();
    gbfsStation.setStationId(ID);
    gbfsStation.setNumDocksAvailable(99);
    gbfsStation.setNumBikesAvailable(4);
    var type = new GBFSVehicleTypesAvailable();
    type.setCount(1);
    type.setVehicleTypeId(TYPE_CAR.id().getId());

    var a = new GBFSVehicleDocksAvailable();
    a.setCount(88);
    a.setVehicleTypeIds(List.of(TYPE_CAR.id().getId()));

    gbfsStation.setVehicleTypesAvailable(List.of(type));
    gbfsStation.setVehicleDocksAvailable(List.of(a));

    var mapper = new GbfsStationStatusMapper(
      Map.of(ID, gbfsStation),
      Map.of(TYPE_CAR.id().getId(), TYPE_CAR)
    );

    var mapped = mapper.mapStationStatus(STATION);

    assertEquals(
      new RentalVehicleEntityCounts(99, List.of(new RentalVehicleTypeCount(TYPE_CAR, 88))),
      mapped.vehicleSpaceCounts()
    );
    assertEquals(Set.of(RentalFormFactor.CAR), mapped.formFactors());
  }

  @Test
  void avaialbleSpacesFromTypesWithoutAvailableVehicles() {
    var gbfsStation = new GBFSStation();
    gbfsStation.setStationId(ID);
    gbfsStation.setNumDocksAvailable(88);
    gbfsStation.setNumBikesAvailable(1);
    var type = new GBFSVehicleTypesAvailable();
    type.setCount(0);
    type.setVehicleTypeId(TYPE_CAR.id().getId());

    gbfsStation.setVehicleTypesAvailable(List.of(type));

    var mapper = new GbfsStationStatusMapper(
      Map.of(ID, gbfsStation),
      Map.of(TYPE_CAR.id().getId(), TYPE_CAR)
    );

    var mapped = mapper.mapStationStatus(STATION);

    assertEquals(
      new RentalVehicleEntityCounts(88, List.of(new RentalVehicleTypeCount(TYPE_CAR, 88))),
      mapped.vehicleSpaceCounts()
    );
    assertEquals(Set.of(RentalFormFactor.CAR), mapped.formFactors());
  }

  @Test
  void noTypes() {
    var gbfsStation = new GBFSStation();
    gbfsStation.setStationId(ID);
    gbfsStation.setNumDocksAvailable(1);
    gbfsStation.setNumBikesAvailable(4);

    var mapper = new GbfsStationStatusMapper(
      Map.of(ID, gbfsStation),
      Map.of(TYPE_CAR.id().getId(), TYPE_CAR)
    );

    var mapped = mapper.mapStationStatus(STATION);
    var bikeType = RentalVehicleType.getDefaultType("F");
    assertEquals(
      new RentalVehicleEntityCounts(1, List.of(new RentalVehicleTypeCount(bikeType, 1))),
      mapped.vehicleSpaceCounts()
    );
    assertEquals(Set.of(RentalFormFactor.BICYCLE), mapped.formFactors());
  }
}
