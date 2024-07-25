package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.support.time.LocalDateRangeUtil;

class LocalDateRangeMapperTest {

  private static final LocalDate DATE = LocalDate.parse("2024-05-27");

  private static List<GraphQLTypes.GraphQLLocalDateRangeInput> noFilterCases() {
    var list = new ArrayList<GraphQLTypes.GraphQLLocalDateRangeInput>();
    list.add(null);
    list.add(new GraphQLTypes.GraphQLLocalDateRangeInput(Map.of()));
    return list;
  }

  @ParameterizedTest
  @MethodSource("noFilterCases")
  void hasNoServiceDateFilter(GraphQLTypes.GraphQLLocalDateRangeInput input) {
    assertFalse(LocalDateRangeUtil.hasServiceDateFilter(input));
  }

  private static List<Map<String, Object>> hasFilterCases() {
    return List.of(Map.of("start", DATE), Map.of("end", DATE), Map.of("start", DATE, "end", DATE));
  }

  @ParameterizedTest
  @MethodSource("hasFilterCases")
  void hasServiceDateFilter(Map<String, Object> params) {
    var input = new GraphQLTypes.GraphQLLocalDateRangeInput(params);
    assertTrue(LocalDateRangeUtil.hasServiceDateFilter(input));
  }
}
