package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSName;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStation;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSVehicleTypesCapacity;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.street.model.RentalFormFactor;

class GbfsStationInformationMapperTest {

  private static final String TEST_STATION_ID = "TEST_STATION_ID";
  private static final String TEST_STATION_NAME = "TEST_STATION_NAME";
  private static final String SYSTEM_ID = "test-system";
  private static final VehicleRentalSystem TEST_SYSTEM = new VehicleRentalSystem(
    SYSTEM_ID,
    I18NString.of("Test System"),
    I18NString.of("Test"),
    I18NString.of("Test Operator"),
    "https://example.com"
  );

  private static final RentalVehicleType TYPE_BIKE = RentalVehicleType.of()
    .withFormFactor(RentalFormFactor.BICYCLE)
    .withId(new FeedScopedId(SYSTEM_ID, "bike"))
    .build();

  private static final RentalVehicleType TYPE_SCOOTER = RentalVehicleType.of()
    .withFormFactor(RentalFormFactor.SCOOTER)
    .withId(new FeedScopedId(SYSTEM_ID, "scooter"))
    .build();

  @Test
  void acceptValidStation() {
    assertTrue(GbfsStationInformationMapper.isValid(validStation()));
  }

  @ParameterizedTest
  @MethodSource("provideInvalidStations")
  void invalidStationsShouldFail(GBFSStation station) {
    assertFalse(GbfsStationInformationMapper.isValid(station));
  }

  @Test
  void stationWithUnknownVehicleTypesInCapacity() {
    var station = validStation();

    // Add vehicle types capacity with both known and unknown vehicle type IDs
    var capacityKnown = new GBFSVehicleTypesCapacity();
    capacityKnown.setVehicleTypeIds(List.of("bike"));
    capacityKnown.setCount(10);

    var capacityUnknown = new GBFSVehicleTypesCapacity();
    capacityUnknown.setVehicleTypeIds(List.of("unknown-type"));
    capacityUnknown.setCount(5);

    station.setVehicleTypesCapacity(List.of(capacityKnown, capacityUnknown));

    var mapper = new GbfsStationInformationMapper(
      TEST_SYSTEM,
      Map.of("bike", TYPE_BIKE, "scooter", TYPE_SCOOTER),
      false,
      false
    );

    var result = mapper.mapStationInformation(station);

    assertNotNull(result);
    assertEquals(TEST_STATION_ID, result.stationId());
    // Should only include the known vehicle type in the capacity map
    assertEquals(1, result.vehicleTypeAreaCapacity().size());
    assertTrue(result.vehicleTypeAreaCapacity().containsKey(TYPE_BIKE));
    assertEquals(10, result.vehicleTypeAreaCapacity().get(TYPE_BIKE));
  }

  private static GBFSStation validStation() {
    return new GBFSStation()
      .withLat(0d)
      .withLon(0d)
      .withStationId(TEST_STATION_ID)
      .withName(List.of(new GBFSName().withText(TEST_STATION_NAME).withLanguage("EN")));
  }

  private static Stream<Arguments> provideInvalidStations() {
    return Stream.of(
      argumentSet("Missing station ID", validStation().withStationId(null)),
      argumentSet("Missing latitude", validStation().withLat(null)),
      argumentSet("Missing longitude", validStation().withLon(null)),
      argumentSet("Missing station name list", validStation().withName(null)),
      argumentSet("Empty station name list", validStation().withName(List.of())),
      argumentSet(
        "Station name list contains null name",
        validStation().withName(
          List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withText(null))
        )
      ),
      argumentSet(
        "Station name list contains empty name",
        validStation().withName(
          List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withText(""))
        )
      ),
      argumentSet(
        "Station name list contains null language",
        validStation().withName(
          List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withLanguage(null))
        )
      ),
      argumentSet(
        "Station name list contains empty language",
        validStation().withName(
          List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withLanguage(""))
        )
      )
    );
  }
}
