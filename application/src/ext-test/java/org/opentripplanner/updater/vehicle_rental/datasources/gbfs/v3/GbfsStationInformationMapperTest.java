package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSName;
import org.mobilitydata.gbfs.v3_0.station_information.GBFSStation;

class GbfsStationInformationMapperTest {

  private static final String TEST_STATION_ID = "TEST_STATION_ID";
  private static final String TEST_STATION_NAME = "TEST_STATION_NAME";

  @Test
  void acceptValidStation() {
    assertTrue(GbfsStationInformationMapper.isValid(validStation()));
  }

  @ParameterizedTest
  @MethodSource("provideInvalidStations")
  void invalidStationsShouldFail(GBFSStation station, String reason) {
    assertFalse(GbfsStationInformationMapper.isValid(station), reason);
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
      Arguments.of(validStation().withStationId(null), "Missing station ID"),
      Arguments.of(validStation().withLat(null), "Missing latitude"),
      Arguments.of(validStation().withLon(null), "Missing longitude"),
      Arguments.of(validStation().withName(null), "Missing station name list"),
      Arguments.of(validStation().withName(List.of()), "Empty station name list"),
      Arguments.of(
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withText(null))
          ),
        "Station name list contains null name"
      ),
      Arguments.of(
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withText(""))
          ),
        "Station name list contains empty name"
      ),
      Arguments.of(
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withLanguage(null))
          ),
        "Station name list contains null language"
      ),
      Arguments.of(
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withLanguage(""))
          ),
        "Station name list contains empty language"
      )
    );
  }
}
