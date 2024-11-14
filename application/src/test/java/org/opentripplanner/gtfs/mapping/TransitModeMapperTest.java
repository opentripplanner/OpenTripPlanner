package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.basic.TransitMode.AIRPLANE;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.CABLE_CAR;
import static org.opentripplanner.transit.model.basic.TransitMode.CARPOOL;
import static org.opentripplanner.transit.model.basic.TransitMode.COACH;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;
import static org.opentripplanner.transit.model.basic.TransitMode.FUNICULAR;
import static org.opentripplanner.transit.model.basic.TransitMode.GONDOLA;
import static org.opentripplanner.transit.model.basic.TransitMode.MONORAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.RAIL;
import static org.opentripplanner.transit.model.basic.TransitMode.SUBWAY;
import static org.opentripplanner.transit.model.basic.TransitMode.TAXI;
import static org.opentripplanner.transit.model.basic.TransitMode.TRAM;
import static org.opentripplanner.transit.model.basic.TransitMode.TROLLEYBUS;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model.basic.TransitMode;

class TransitModeMapperTest {

  static Stream<Arguments> testCases() {
    return Stream.of(
      // base GTFS route types
      // https://gtfs.org/documentation/schedule/reference/#routestxt
      Arguments.of(0, TRAM),
      Arguments.of(1, SUBWAY),
      Arguments.of(2, RAIL),
      Arguments.of(3, BUS),
      Arguments.of(4, FERRY),
      Arguments.of(5, CABLE_CAR),
      Arguments.of(6, GONDOLA),
      Arguments.of(7, FUNICULAR),
      Arguments.of(11, TROLLEYBUS),
      Arguments.of(12, MONORAIL),
      // extended route types
      // https://developers.google.com/transit/gtfs/reference/extended-route-types
      // https://groups.google.com/g/gtfs-changes/c/keT5rTPS7Y0/m/71uMz2l6ke0J?pli=1
      Arguments.of(100, RAIL),
      Arguments.of(199, RAIL),
      Arguments.of(200, COACH),
      Arguments.of(299, COACH),
      Arguments.of(400, RAIL),
      Arguments.of(401, SUBWAY),
      Arguments.of(402, SUBWAY),
      Arguments.of(403, RAIL),
      Arguments.of(404, RAIL),
      Arguments.of(405, MONORAIL),
      Arguments.of(500, SUBWAY),
      Arguments.of(599, SUBWAY),
      Arguments.of(600, SUBWAY),
      Arguments.of(699, SUBWAY),
      Arguments.of(700, BUS),
      Arguments.of(799, BUS),
      Arguments.of(800, TROLLEYBUS),
      Arguments.of(899, TROLLEYBUS),
      Arguments.of(900, TRAM),
      Arguments.of(999, TRAM),
      Arguments.of(1000, FERRY),
      Arguments.of(1099, FERRY),
      Arguments.of(1100, AIRPLANE),
      Arguments.of(1199, AIRPLANE),
      Arguments.of(1200, FERRY),
      Arguments.of(1299, FERRY),
      Arguments.of(1300, GONDOLA),
      Arguments.of(1399, GONDOLA),
      Arguments.of(1400, FUNICULAR),
      Arguments.of(1499, FUNICULAR),
      Arguments.of(1500, TAXI),
      Arguments.of(1510, TAXI),
      Arguments.of(1551, CARPOOL),
      Arguments.of(1555, CARPOOL),
      Arguments.of(1560, CARPOOL),
      Arguments.of(1561, TAXI),
      Arguments.of(1580, TAXI)
    );
  }

  @ParameterizedTest(name = "{0} should map to {1}")
  @MethodSource("testCases")
  void map(int mode, TransitMode expectedMode) {
    assertEquals(expectedMode, TransitModeMapper.mapMode(mode));
  }
}
