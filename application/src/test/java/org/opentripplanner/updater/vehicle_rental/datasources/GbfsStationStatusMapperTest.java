package org.opentripplanner.updater.vehicle_rental.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.CAR;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;
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
import org.opentripplanner.service.vehiclerental.model.ReturnPolicy;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;

class GbfsStationStatusMapperTest {

  private static final RentalVehicleType TYPE_CAR = RentalVehicleType.of()
    .withFormFactor(CAR)
    .withId(id("tc"))
    .build();
  private static final String ID = "s1";
  private static final VehicleRentalStation STATION = VehicleRentalStation.of()
    .withId(id(ID))
    .build();

  @Test
  void availableSpacesFromVehicles() {
    final var gbfsStation = station();
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

    assertEquals(new RentalVehicleEntityCounts(3, List.of()), mapped.vehicleSpaceCounts());

    assertEquals(Set.of(CAR), mapped.formFactors());
    assertSame(ReturnPolicy.ANY_TYPE, mapped.returnPolicy());

    assertDropOffForAnyType(mapped);
  }

  @Test
  void availableSpacesFromTypes() {
    var gbfsStation = station();
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
    assertEquals(Set.of(CAR), mapped.formFactors());
    assertSame(ReturnPolicy.SPECIFIC_TYPES, mapped.returnPolicy());

    assertFalse(mapped.canDropOffFormFactor(BICYCLE, false));
    assertTrue(mapped.canDropOffFormFactor(CAR, false));
    assertFalse(mapped.canDropOffFormFactor(SCOOTER, false));
  }

  @Test
  void availableSpacesFromTypesWithoutAvailableVehicles() {
    var gbfsStation = station();
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

    assertEquals(new RentalVehicleEntityCounts(88, List.of()), mapped.vehicleSpaceCounts());
    assertEquals(Set.of(CAR), mapped.formFactors());
    assertSame(ReturnPolicy.ANY_TYPE, mapped.returnPolicy());

    assertDropOffForAnyType(mapped);
  }

  @Test
  void noTypes() {
    var gbfsStation = station();
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
    assertEquals(Set.of(BICYCLE), mapped.formFactors());
    assertSame(ReturnPolicy.ANY_TYPE, mapped.returnPolicy());
    assertDropOffForAnyType(mapped);
  }

  private static GBFSStation station() {
    var gbfsStation = new GBFSStation();
    gbfsStation.setStationId(ID);
    gbfsStation.setIsReturning(true);
    return gbfsStation;
  }

  private static void assertDropOffForAnyType(VehicleRentalStation mapped) {
    assertTrue(mapped.canDropOffFormFactor(BICYCLE, false));
    assertTrue(mapped.canDropOffFormFactor(CAR, false));
    assertTrue(mapped.canDropOffFormFactor(SCOOTER, false));
  }
}
