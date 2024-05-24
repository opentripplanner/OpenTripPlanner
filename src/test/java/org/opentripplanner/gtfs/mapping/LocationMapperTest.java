package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Location;
import org.opentripplanner.transit.service.StopModel;

class LocationMapperTest {

  static Stream<Arguments> testCases() {
    return Stream.of(Arguments.of(null, true), Arguments.of("a name", false));
  }

  @ParameterizedTest(name = "a name of <{0}> should set bogusName={1}")
  @MethodSource("testCases")
  void testMapping(String name, boolean isBogusName) {
    var gtfsLocation = new Location();
    gtfsLocation.setId(new AgencyAndId("1", "zone-3"));
    gtfsLocation.setName(name);
    gtfsLocation.setGeometry(
      new Polygon(
        List.of(new LngLatAlt(1, 1), new LngLatAlt(1, 2), new LngLatAlt(1, 3), new LngLatAlt(1, 1))
      )
    );

    var mapper = new LocationMapper(StopModel.of());
    var flexLocation = mapper.map(gtfsLocation);

    assertEquals(isBogusName, flexLocation.hasFallbackName());
  }
}
