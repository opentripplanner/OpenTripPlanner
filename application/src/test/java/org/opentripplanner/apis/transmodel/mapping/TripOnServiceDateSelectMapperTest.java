package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.list;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.map;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.apis.support.InvalidInputException;

class TripOnServiceDateSelectMapperTest {

  @SuppressWarnings("unchecked")
  static Stream<Arguments> mapSelectRequestCases() {
    return Stream.of(
      argumentSet("lines", map("lines", list("F:Line:1")), "(routes: [F:Line:1])"),
      argumentSet("authorities", map("authorities", list("F:Auth:1")), "(agencies: [F:Auth:1])"),
      argumentSet(
        "mode",
        map(entry("transportModes", list(map("transportMode", BUS)))),
        "(transportModes: [BUS])"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("mapSelectRequestCases")
  void mapSelectRequest(Map<String, List<?>> input, String expected) {
    var mapper = new TripOnServiceDateSelectMapper(new DefaultFeedIdMapper());
    assertEquals(expected, mapper.mapSelectRequest(input).toString());
  }

  @SuppressWarnings("unchecked")
  static Stream<Arguments> emptyListRejectedCases() {
    return Stream.of(
      argumentSet("emptySelector", map()),
      argumentSet("emptyLines", map("lines", list())),
      argumentSet("emptyAuthorities", map("authorities", list())),
      argumentSet("emptyTransportModes", map(entry("transportModes", list()))),
      argumentSet("emptyModeEntry", map(entry("transportModes", list(map()))))
    );
  }

  @ParameterizedTest
  @MethodSource("emptyListRejectedCases")
  void emptyListsAreRejected(Map<String, List<?>> input) {
    var mapper = new TripOnServiceDateSelectMapper(new DefaultFeedIdMapper());
    assertThrows(InvalidInputException.class, () -> mapper.mapSelectRequest(input));
  }
}
