package org.opentripplanner.gtfs.graphbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.onebusaway.csv_entities.CsvInputSource;
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

  @Test
  void readFeedIdFromFeedInfo() {
    var source = feedInfoSource("feed_id,feed_lang\nfeed:1,en\n");
    assertEquals("feed1", GtfsFeedIdResolver.fromGtfsFeed(source, new Object()));
  }

  /**
   * A feed_info.txt with malformed quoting must not abort the graph build, a feed id is
   * generated instead.
   */
  @Test
  void generateFeedIdIfFeedInfoIsMalformed() {
    var source = feedInfoSource("feed_id,feed_lang\n\"feed\"1,en\n");
    String feedId = GtfsFeedIdResolver.fromGtfsFeed(source, new Object());
    assertTrue(feedId.matches(NUMBERS_ONLY_REGEX), "'%s' is not an integer.".formatted(feedId));
  }

  private static CsvInputSource feedInfoSource(String feedInfo) {
    return new CsvInputSource() {
      @Override
      public boolean hasResource(String name) {
        return name.equals("feed_info.txt");
      }

      @Override
      public InputStream getResource(String name) {
        return new ByteArrayInputStream(feedInfo.getBytes(StandardCharsets.UTF_8));
      }

      @Override
      public void close() {}
    };
  }
}
