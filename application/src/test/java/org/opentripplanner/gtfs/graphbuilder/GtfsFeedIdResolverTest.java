package org.opentripplanner.gtfs.graphbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.opentripplanner.ConstantsForTests;

class GtfsFeedIdResolverTest {

  private static final String NUMBERS_ONLY_REGEX = "^\\d+$";

  static Stream<Arguments> emptyCases() {
    return Stream.of(null, "", "     ", "\n", "  ").map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("emptyCases")
  void normalizeIdAutogenerateNumber(String id) {
    String feedId = GtfsFeedIdResolver.normalizeId(id);
    assertTrue(feedId.matches(NUMBERS_ONLY_REGEX), "'%s' is not an integer.".formatted(feedId));
  }

  @Test
  void normalizeIdRemoveColon() {
    assertEquals("feedid", GtfsFeedIdResolver.normalizeId("feed:id:"));
  }

  @Test
  void normalizeIdKeepUnderscore() {
    assertEquals("feed_id_", GtfsFeedIdResolver.normalizeId("feed_id_"));
  }

  @Test
  void verifyTheSameFeedIdIsGeneratedIfTheFeedIsTheSame() throws IOException {
    var dataSourceid = ConstantsForTests.SIMPLE_GTFS.toURI();
    var reader = new GtfsReader();
    reader.setInputLocation(ConstantsForTests.SIMPLE_GTFS);
    var firstFeedId = GtfsFeedIdResolver.fromGtfsFeed(reader.getInputSource(), dataSourceid);
    var secondFeedId = GtfsFeedIdResolver.fromGtfsFeed(reader.getInputSource(), dataSourceid);
    assertEquals(firstFeedId, secondFeedId);
  }
}
