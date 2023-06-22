package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;

class GtfsFeedIdTest {

  private static final String NUMBERS_ONLY_REGEX = "^\\d+$";

  static Stream<Arguments> emptyCases = Stream.of(null, "", "     ", "\n", "  ").map(Arguments::of);

  @ParameterizedTest
  @VariableSource("emptyCases")
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

  @Nonnull
  private static String feedId(String input) {
    var id = new GtfsFeedId.Builder().id(input).build().getId();
    assertNotNull(id);
    return id;
  }
}
