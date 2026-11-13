package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.list;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.map;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;
import org.opentripplanner.apis.support.InvalidInputException;

class TripTimeOnDateFilterMapperTest {

  private static final TripTimeOnDateFilterMapper MAPPER = new TripTimeOnDateFilterMapper(
    new DefaultFeedIdMapper()
  );

  @SuppressWarnings("unchecked")
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
      )
    );
  }

  @ParameterizedTest
  @MethodSource("mapFiltersCases")
  void mapFilters(List<Map<String, ?>> input, String expected) {
    var result = MAPPER.mapFilters(input);
    assertEquals(expected, result.toString());
  }

  @SuppressWarnings("unchecked")
  static Stream<Arguments> emptyListRejectedCases() {
    return Stream.of(
      argumentSet("emptyFilterList", List.<Map<String, ?>>of()),
      argumentSet("emptySelectArray", list(map("select", list()))),
      argumentSet("emptyNotArray", list(map("not", list()))),
      argumentSet("emptyLinesInSelect", list(map("select", list(map("lines", list()))))),
      argumentSet(
        "emptyAuthoritiesInSelect",
        list(map("select", list(map("authorities", list()))))
      ),
      argumentSet(
        "emptyTransportModesInSelect",
        list(map("select", list(map(entry("transportModes", list())))))
      ),
      argumentSet("emptyLinesInNot", list(map("not", list(map("lines", list()))))),
      argumentSet("emptyAuthoritiesInNot", list(map("not", list(map("authorities", list()))))),
      argumentSet(
        "emptyTransportModesInNot",
        list(map("not", list(map(entry("transportModes", list())))))
      ),
      argumentSet("emptyFilter", list(map())),
      argumentSet("emptySelectorInSelect", list(map("select", list(map())))),
      argumentSet("emptySelectorInNot", list(map("not", list(map())))),
      argumentSet(
        "emptySelectorInSelectAndNot",
        list(map(entry("select", list(map())), entry("not", list(map()))))
      ),
      argumentSet(
        "emptyModeEntryInSelect",
        list(map("select", list(map(entry("transportModes", list(map()))))))
      ),
      argumentSet(
        "emptyModeEntryInNot",
        list(map("not", list(map(entry("transportModes", list(map()))))))
      )
    );
  }

  @ParameterizedTest
  @MethodSource("emptyListRejectedCases")
  void emptyListsAreRejected(List<Map<String, ?>> input) {
    assertThrows(InvalidInputException.class, () -> MAPPER.mapFilters(input));
  }

  @SuppressWarnings("unchecked")
  static Stream<Arguments> explicitNullIsOmittedCases() {
    return Stream.of(
      argumentSet(
        "nullNotNextToSelect",
        list(withNulls(map("select", list(map("lines", list("F:Line:1")))), "not")),
        "[(select: [(routes: [F:Line:1])])]"
      ),
      argumentSet(
        "nullSelectNextToNot",
        list(withNulls(map("not", list(map("lines", list("F:Line:1")))), "select")),
        "[(not: [(routes: [F:Line:1])])]"
      ),
      argumentSet(
        "nullAuthoritiesNextToLines",
        list(map("select", list(withNulls(map("lines", list("F:Line:1")), "authorities")))),
        "[(select: [(routes: [F:Line:1])])]"
      ),
      argumentSet(
        "nullLinesNextToAuthorities",
        list(map("select", list(withNulls(map("authorities", list("F:Auth:1")), "lines")))),
        "[(select: [(agencies: [F:Auth:1])])]"
      ),
      argumentSet(
        "nullTransportModesNextToLines",
        list(map("not", list(withNulls(map("lines", list("F:Line:1")), "transportModes")))),
        "[(not: [(routes: [F:Line:1])])]"
      ),
      argumentSet(
        "nullTransportSubModesMatchesAllSubModes",
        list(
          map(
            "select",
            list(
              map(
                entry(
                  "transportModes",
                  list(withNulls(map("transportMode", BUS), "transportSubModes"))
                )
              )
            )
          )
        ),
        "[(select: [(transportModes: [BUS])])]"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("explicitNullIsOmittedCases")
  void explicitNullIsTreatedAsOmitted(List<Map<String, ?>> input, String expected) {
    assertEquals(expected, MAPPER.mapFilters(input).toString());
  }

  @SuppressWarnings("unchecked")
  static Stream<Arguments> nothingLeftAfterNullsCases() {
    return Stream.of(
      argumentSet(
        "nullSelectAndNot",
        list(withNulls(map(), "select", "not")),
        "A filter must have at least one of 'select' or 'not'."
      ),
      argumentSet(
        "nullSelectOnly",
        list(withNulls(map(), "select")),
        "A filter must have at least one of 'select' or 'not'."
      ),
      argumentSet(
        "nullLinesOnly",
        list(map("select", list(withNulls(map(), "lines")))),
        "A selector cannot be empty"
      ),
      argumentSet(
        "nullLinesAuthoritiesAndModes",
        list(map("not", list(withNulls(map(), "lines", "authorities", "transportModes")))),
        "A selector cannot be empty"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("nothingLeftAfterNullsCases")
  void nullsLeavingNothingAreRejected(List<Map<String, ?>> input, String message) {
    var ex = assertThrows(InvalidInputException.class, () -> MAPPER.mapFilters(input));
    assertEquals(message, ex.getMessage());
  }

  @Test
  void rejectUnknownTransportMode() {
    // The GraphQL TransportMode enum maps "unknown" to a String, not to a TransitMode
    assertThrows(InvalidInputException.class, () ->
      MAPPER.mapFilters(
        list(
          map("select", list(map(entry("transportModes", list(map("transportMode", "unknown"))))))
        )
      )
    );
  }

  /**
   * A copy of the given GraphQL input object where the given fields are set to an explicit
   * {@code null}, which is how a client that sends {@code field: null} shows up in the mapper.
   */
  private static Map<String, Object> withNulls(Map<String, ?> input, String... nullFields) {
    var result = new HashMap<String, Object>(input);
    for (var field : nullFields) {
      result.put(field, null);
    }
    return result;
  }
}
