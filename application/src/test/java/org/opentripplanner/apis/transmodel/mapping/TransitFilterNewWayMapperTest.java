package org.opentripplanner.apis.transmodel.mapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.list;
import static org.opentripplanner.apis.transmodel._support.RequestHelper.map;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.api.model.transit.DefaultFeedIdMapper;

class TransitFilterNewWayMapperTest {

  private static final TransitFilterNewWayMapper MAPPER = new TransitFilterNewWayMapper(
    new DefaultFeedIdMapper()
  );

  @Test
  void mapEmptyFilter() {
    var result = MAPPER.mapFilter(List.of());
    assertEquals("[]", result.toString());
  }

  @Test
  void mapIncludeLineFilter() {
    var result = MAPPER.mapFilter(list(map("select", list(map("lines", list("F:Line:1"))))));
    assertEquals("[(select: [(transportModes: EMPTY, routes: [F:Line:1])])]", result.toString());
  }

  @Test
  void mapFilterWithModes() {
    var result = MAPPER.mapFilter(
      list(map("select", list(map(entry("transportModes", list(map("transportMode", BUS)))))))
    );

    assertEquals("[(select: [(transportModes: [BUS])])]", result.toString());
  }

  @Test
  void mapExcludeLineFilter() {
    var result = MAPPER.mapFilter(list(map("not", list(map("lines", list("F:Line:1"))))));
    assertEquals("[(not: [(transportModes: EMPTY, routes: [F:Line:1])])]", result.toString());
  }

  private static <T> Map.Entry<String, List<T>> e(String key, T... values) {
    return entry(key, Arrays.asList(values));
  }
}
