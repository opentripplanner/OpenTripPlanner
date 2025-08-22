package org.opentripplanner.updater.vehicle_rental.datasources;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.service.vehiclerental.model.ReturnPolicy.ANY_TYPE;
import static org.opentripplanner.service.vehiclerental.model.ReturnPolicy.SPECIFIC_TYPES;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.CAR;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSStation;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSVehicleDocksAvailable;
import org.mobilitydata.gbfs.v2_3.station_status.GBFSVehicleTypesAvailable;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleEntityCounts;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleTypeCount;
import org.opentripplanner.service.vehiclerental.model.ReturnPolicy;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.street.model.RentalFormFactor;

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

    var spaces = mapped.vehicleSpaceCounts();
    assertEquals(3, spaces.total());
    assertThat(spaces.byType()).isEmpty();

    assertEquals(Set.of(CAR), mapped.formFactors());
    assertSame(ANY_TYPE, mapped.returnPolicy());

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

    var spaces = mapped.vehicleSpaceCounts();
    assertEquals(99, spaces.total());
    assertThat(spaces.byType()).containsExactly(new RentalVehicleTypeCount(TYPE_CAR, 88));
    assertEquals(Set.of(CAR), mapped.formFactors());
    assertSame(SPECIFIC_TYPES, mapped.returnPolicy());

    // can drop off car
    assertTrue(mapped.canDropOffFormFactor(CAR, false));
    // but not the other types
    assertFalse(mapped.canDropOffFormFactor(BICYCLE, false));
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

    var spaces = mapped.vehicleSpaceCounts();
    assertEquals(88, spaces.total());
    assertThat(spaces.byType()).isEmpty();
    assertEquals(Set.of(CAR), mapped.formFactors());
    assertSame(ANY_TYPE, mapped.returnPolicy());

    assertDropOffForAnyType(mapped);
  }

  @Test
  void noTypes() {
    var gbfsStation = station();
    gbfsStation.setNumDocksAvailable(1);
    gbfsStation.setNumBikesAvailable(4);

    var mapper = new GbfsStationStatusMapper(Map.of(ID, gbfsStation), Map.of());

    var mapped = mapper.mapStationStatus(STATION);

    var spaces = mapped.vehicleSpaceCounts();
    assertEquals(1, spaces.total());
    assertThat(spaces.byType()).isEmpty();
    assertEquals(Set.of(BICYCLE), mapped.formFactors());
    assertSame(ANY_TYPE, mapped.returnPolicy());
    assertDropOffForAnyType(mapped);
  }

  @ParameterizedTest
  @EnumSource(RentalFormFactor.class)
  void noSpaces(RentalFormFactor formFactor) {
    var gbfsStation = station();
    gbfsStation.setNumDocksAvailable(0);
    gbfsStation.setNumBikesAvailable(4);

    var mapper = new GbfsStationStatusMapper(Map.of(ID, gbfsStation), Map.of());

    var mapped = mapper.mapStationStatus(STATION);

    var spaces = mapped.vehicleSpaceCounts();
    assertEquals(0, spaces.total());
    assertThat(spaces.byType()).isEmpty();
    assertEquals(Set.of(BICYCLE), mapped.formFactors());
    assertSame(ANY_TYPE, mapped.returnPolicy());

    assertFalse(mapped.canDropOffFormFactor(formFactor, true));
    assertTrue(mapped.canDropOffFormFactor(formFactor, false));
  }

  private static GBFSStation station() {
    var gbfsStation = new GBFSStation();
    gbfsStation.setStationId(ID);
    gbfsStation.setIsReturning(true);
    return gbfsStation;
  }

  private static void assertDropOffForAnyType(VehicleRentalStation mapped) {
    Stream.of(true, false).forEach(realtime -> {
      assertTrue(mapped.canDropOffFormFactor(BICYCLE, realtime));
      assertTrue(mapped.canDropOffFormFactor(CAR, realtime));
      assertTrue(mapped.canDropOffFormFactor(SCOOTER, realtime));
    });
  }
}
