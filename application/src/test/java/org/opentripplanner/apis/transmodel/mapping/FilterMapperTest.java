package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.list;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.map;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.support.InvalidInputException;

class FilterMapperTest {

  // Dummy mapper selector mapper.
  // Returns "{}" regardless of selector content so tests can focus on filter structure only.
  private static final Function<Map<String, List<?>>, String> SELECT_MAPPER = _ -> "{}";

  @SuppressWarnings("unchecked")
  static Stream<Arguments> mapFiltersCases() {
    return Stream.of(
      argumentSet(
        "selectAndNot",
        list(
          map(
            entry("select", list(map("key", list("value")))),
            entry("not", list(map("key", list("value"))))
          )
        ),
        "[(select: [{}], not: [{}])]"
      ),
      argumentSet(
        "multipleFilters",
        list(
          map("select", list(map("key", list("value")))),
          map("not", list(map("key", list("value"))))
        ),
        "[(select: [{}]), (not: [{}])]"
      )
    );
  }

  @ParameterizedTest
  @MethodSource("mapFiltersCases")
  void mapFilters(List<Map<String, ?>> input, String expected) {
    var result = FilterMapper.mapFilters(input, SELECT_MAPPER);
    assertEquals(expected, result.toString());
  }

  @SuppressWarnings("unchecked")
  static Stream<Arguments> emptyListRejectedCases() {
    return Stream.of(
      argumentSet("emptyFilterList", List.<Map<String, ?>>of()),
      argumentSet("emptySelectArray", list(map("select", list()))),
      argumentSet("emptyNotArray", list(map("not", list()))),
      argumentSet("emptyFilter", list(map()))
    );
  }

  @ParameterizedTest
  @MethodSource("emptyListRejectedCases")
  void emptyListsAreRejected(List<Map<String, ?>> input) {
    assertThrows(InvalidInputException.class, () -> FilterMapper.mapFilters(input, SELECT_MAPPER));
  }
}
