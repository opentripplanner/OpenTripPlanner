package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

class TripTimeOnDateFilterMapperTest {

  private static final TripTimeOnDateFilterMapper MAPPER = new TripTimeOnDateFilterMapper(
    new DefaultFeedIdMapper()
  );

  static Stream<Arguments> mapFiltersCases() {
    return Stream.of(
      argumentSet(
        "selectByLine",
        list(map("select", list(map("lines", list("F:Line:1"))))),
        "[(select: [(routes: [F:Line:1])])]"
      ),
      argumentSet(
        "selectByAuthority",
        list(map("select", list(map("authorities", list("F:Auth:1"))))),
        "[(select: [(agencies: [F:Auth:1])])]"
      ),
      argumentSet(
        "selectByMode",
        list(map("select", list(map(entry("transportModes", list(map("transportMode", BUS))))))),
        "[(select: [(transportModes: [BUS])])]"
      ),
      argumentSet(
        "notByLine",
        list(map("not", list(map("lines", list("F:Line:1"))))),
        "[(not: [(routes: [F:Line:1])])]"
      ),
      argumentSet(
        "notByAuthority",
        list(map("not", list(map("authorities", list("F:Auth:1"))))),
        "[(not: [(agencies: [F:Auth:1])])]"
      ),
      argumentSet(
        "selectAndNot",
        list(
          map(
            entry("select", list(map("lines", list("F:Line:1")))),
            entry("not", list(map("authorities", list("F:Auth:1"))))
          )
        ),
        "[(select: [(routes: [F:Line:1])], not: [(agencies: [F:Auth:1])])]"
      ),
      argumentSet(
        "multipleFilters",
        list(
          map("select", list(map("lines", list("F:Line:1")))),
          map("not", list(map("authorities", list("F:Auth:2"))))
        ),
        "[(select: [(routes: [F:Line:1])]), (not: [(agencies: [F:Auth:2])])]"
      ),
      // Empty list cases
      argumentSet("emptyFilterList", List.<Map<String, ?>>of(), "[]"),
      argumentSet("emptySelectArray", list(map("select", list())), "[ALL]"),
      argumentSet("emptyNotArray", list(map("not", list())), "[ALL]"),
      argumentSet("emptySelectorInSelect", list(map("select", list(map()))), "[(select: [()])]"),
      argumentSet("emptySelectorInNot", list(map("not", list(map()))), "[(not: [()])]"),
      argumentSet(
        "emptySelectorInSelectAndNot",
        list(map(entry("select", list(map())), entry("not", list(map())))),
        "[(select: [()], not: [()])]"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("mapFiltersCases")
  void mapFilters(List<Map<String, ?>> input, String expected) {
    var result = MAPPER.mapFilters(input);
    assertEquals(expected, result.toString());
  }
}
