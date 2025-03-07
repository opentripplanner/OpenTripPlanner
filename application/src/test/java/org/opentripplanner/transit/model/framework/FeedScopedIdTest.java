package org.opentripplanner.transit.model.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeedScopedIdTest {

  private static final List<FeedScopedId> TRIMET_123 = List.of(new FeedScopedId("trimet", "123"));

  @Test
  void ofNullable() {
    assertEquals(new FeedScopedId("FEED", "ID"), FeedScopedId.ofNullable("FEED", "ID"));
    assertNull(FeedScopedId.ofNullable("FEED", null));
  }

  @ParameterizedTest
  @ValueSource(
    strings = { "trimet:123", "trimet:123 ", "trimet:123, ", ",trimet:123 , ", " trimet:123 " }
  )
  void parseList(String input) {
    assertEquals(TRIMET_123, FeedScopedId.parseList(input));
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      ",trimet:123 , ,\u200B,",
      "\u200Btrimet:123",
      "\u200B\u200Btri\u200Bmet:123\u200B",
      "\ntrimet:123\t",
      "\ntri\nmet:123\t",
    }
  )
  void throwExceptionForInvisibleChar(String input) {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      FeedScopedId.parseList(input);
    });
  }
}
