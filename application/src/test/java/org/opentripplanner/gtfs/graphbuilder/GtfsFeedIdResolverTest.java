package org.opentripplanner.gtfs.graphbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GtfsFeedIdResolverTest {

  private static final String NUMBERS_ONLY_REGEX = "^\\d+$";

  static Stream<Arguments> emptyCases() {
    return Stream.of(null, "", "     ", "\n", "  ").map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("emptyCases")
  void autogenerateNumber(String id) {
    String feedId = feedId(id);
    assertTrue(feedId.matches(NUMBERS_ONLY_REGEX), "'%s' is not an integer.".formatted(feedId));
  }

  @Test
  void removeColon() {
    assertEquals("feedid", feedId("feed:id:"));
  }

  @Test
  void keepUnderscore() {
    assertEquals("feed_id_", feedId("feed_id_"));
  }

  private static String feedId(String input) {
    var id = GtfsFeedIdResolver.normalizeId(input);
    assertNotNull(id);
    return id;
  }
}
