package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

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
  void invalidStationsShouldFail(GBFSStation station) {
    assertFalse(GbfsStationInformationMapper.isValid(station));
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
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withText(null))
          )
      ),
      argumentSet(
        "Station name list contains empty name",
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withText(""))
          )
      ),
      argumentSet(
        "Station name list contains null language",
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withLanguage(null))
          )
      ),
      argumentSet(
        "Station name list contains empty language",
        validStation()
          .withName(
            List.of(new GBFSName().withText(TEST_STATION_NAME), new GBFSName().withLanguage(""))
          )
      )
    );
  }
}
