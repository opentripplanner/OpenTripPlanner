package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLTransitFilterInput;
import org.opentripplanner.transit.model.basic.MainAndSubMode;

class FilterMapperTest {

  @Test
  void map() {
    var filter = new GraphQLTransitFilterInput(
      Map.of(
        "exclude",
        List.of(Map.of("routes", List.of("feed:A"))),
        "include",
        List.of(Map.of("agencies", List.of("feed:A")))
      )
    );
    var result = FilterMapper.mapFilters(MainAndSubMode.all(), List.of(filter)).stream().toList();
    assertEquals(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: ALL-MAIN-MODES, agencies: [feed:A]}], not: [SelectRequest{transportModes: [], routes: [feed:A]}]}]",
      result.toString()
    );
  }

  private static List<Map<String, Object>> emptyListCases() {
    var emptyAgencies = List.of(Map.of("routes", List.of("feed:A"), "agencies", List.of()));
    List<String> listWithNull = new ArrayList<>();
    listWithNull.add(null);

    return List.of(
      Map.of(
        "exclude",
        List.of(Map.of("routes", List.of())),
        "include",
        List.of(Map.of("routes", List.of()))
      ),
      Map.of(
        "exclude",
        List.of(Map.of("agencies", List.of())),
        "include",
        List.of(Map.of("agencies", List.of()))
      ),
      Map.of("include", List.of(Map.of("routes", List.of()))),
      Map.of("exclude", emptyAgencies),
      Map.of("include", emptyAgencies),
      Map.of("include", List.of()),
      Map.of("exclude", List.of()),
      Map.of("include", List.of(Map.of())),
      Map.of("exclude", List.of(Map.of())),
      Map.of("exclude", List.of(Map.of("routes", listWithNull))),
      Map.of("exclude", List.of(Map.of("agencies", listWithNull))),
      Map.of()
    );
  }

  @ParameterizedTest
  @MethodSource("emptyListCases")
  void emptyList(Map<String, Object> args) {
    var input = new GraphQLTransitFilterInput(args);
    assertThrows(IllegalArgumentException.class, () ->
      FilterMapper.mapFilters(MainAndSubMode.all(), List.of(input))
    );
  }
}
