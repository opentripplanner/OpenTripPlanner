package org.opentripplanner.inspector.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class KeyValueTest {

  private static List<Arguments> cases() {
    return List.of(
      Arguments.of("MONDAY", DayOfWeek.MONDAY),
      Arguments.of("1:1", new FeedScopedId("1", "1")),
      Arguments.of(1, 1),
      Arguments.of(null, null)
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  void kv(Object expected, Object value) {
    assertEquals(new KeyValue("k", expected), KeyValue.kv("k", value));
  }
}
