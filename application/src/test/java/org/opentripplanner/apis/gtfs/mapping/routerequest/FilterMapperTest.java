package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLPlanFilterInput;

class FilterMapperTest {

  @Test
  void map() {
    var filter = new GraphQLPlanFilterInput(
      Map.of(
        "not",
        List.of(Map.of("routes", List.of("feed:A"))),
        "select",
        List.of(
          Map.of("agencies", List.of("feed:A")),
          Map.of("transportModes", List.of("RAIL"))
        )
      )
    );
    var result = FilterMapper.mapFilters(List.of(filter));
    assertEquals(
      "[TransitFilterRequest{select: [SelectRequest{transportModes: [], agencies: [feed:A]}, SelectRequest{transportModes: [RAIL]}], not: [SelectRequest{transportModes: [], routes: [feed:A]}]}]",
      result.toString()
    );
  }

  private static List<Map<String, Object>> emptyListCases() {
    var emptyAgencies = List.of(
      Map.of("transportModes", List.of("RAIL"), "routes", List.of("feed:A"), "agencies", List.of())
    );
    List<String> listWithNull = new ArrayList<>();
    listWithNull.add(null);

    return List.of(
      Map.of(
        "not",
        List.of(Map.of("routes", List.of())),
        "select",
        List.of(Map.of("routes", List.of()))
      ),
      Map.of(
        "not",
        List.of(Map.of("agencies", List.of())),
        "select",
        List.of(Map.of("agencies", List.of()))
      ),
      Map.of(
        "not",
        List.of(Map.of("transportModes", List.of())),
        "select",
        List.of(Map.of("transportModes", List.of()))
      ),
      Map.of("select", List.of(Map.of("transportModes", List.of("RAIL"), "routes", List.of()))),
      Map.of("not", emptyAgencies),
      Map.of("select", emptyAgencies),
      Map.of("select", List.of()),
      Map.of("not", List.of()),
      Map.of("not", List.of(Map.of("routes", listWithNull))),
      Map.of("not", List.of(Map.of("agencies", listWithNull))),
      Map.of()
    );
  }

  @ParameterizedTest
  @MethodSource("emptyListCases")
  void emptyList(Map<String, Object> args) {
    var input = new GraphQLPlanFilterInput(args);
    assertThrows(IllegalArgumentException.class, () -> FilterMapper.mapFilters(List.of(input)));
  }
}
