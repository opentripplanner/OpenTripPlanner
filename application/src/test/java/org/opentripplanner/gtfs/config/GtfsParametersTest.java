package org.opentripplanner.gtfs.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.gtfs.config.GtfsDefaultParameters.DEFAULT;
import static org.opentripplanner.transit.model.site.StopTransferPriority.DISCOURAGED;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.StopTransferPriority;

/**
 * This unit-tests tests both {@link GtfsDefaultParameters} and {@link GtfsFeedParameters}.
 */
class GtfsParametersTest {

  private final boolean BLOCK_BASED_INTERLINEING = !DEFAULT.blockBasedInterlining();
  private final boolean DISCARD_MIN_TRANSFERTIMES = !DEFAULT.discardMinTransferTimes();
  private final StopTransferPriority STATION_TRANSFER_PREFERENCE = DISCOURAGED;
  private final boolean REMOVE_REPEATED_STOPS = !DEFAULT.removeRepeatedStops();
  private final int MAX_INTERLINE_DISTANCE = DEFAULT.maxInterlineDistance() + 100;
  private final String FEED = "FEED";
  private final URI SOURCE;

  private final String EXPECTED_BODY_STRING =
    "" +
    "removeRepeatedStops: " +
    REMOVE_REPEATED_STOPS +
    ", " +
    "stationTransferPreference: " +
    STATION_TRANSFER_PREFERENCE +
    ", " +
    "discardMinTransferTimes: " +
    DISCARD_MIN_TRANSFERTIMES +
    ", " +
    "blockBasedInterlining: " +
    BLOCK_BASED_INTERLINEING +
    ", " +
    "maxInterlineDistance: " +
    MAX_INTERLINE_DISTANCE;
  private final String EXPECTED_DEFAULT_TO_STRING =
    "GtfsDefaultParameters{" + EXPECTED_BODY_STRING + "}";

  private final String EXPECTED_FEED_TO_STRING;
  private final String EXPECTED_FEED_WITHOUT_FEED_ID_TO_STRING;

  private final GtfsDefaultParameters SUBJECT_DEFAULT;
  private final GtfsFeedParameters SUBJECT_FEED;
  private final GtfsFeedParameters SUBJECT_FEED_WITHOUT_FEED_ID;

  GtfsParametersTest() {
    try {
      SOURCE = new URI("http://a.place.org/feed");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    EXPECTED_FEED_TO_STRING =
      "GtfsFeedParameters{" +
      "feedId: '" +
      FEED +
      "', " +
      "source: " +
      SOURCE +
      ", " +
      EXPECTED_BODY_STRING +
      "}";
    EXPECTED_FEED_WITHOUT_FEED_ID_TO_STRING =
      "GtfsFeedParameters{" + "source: " + SOURCE + ", " + EXPECTED_BODY_STRING + "}";

    SUBJECT_DEFAULT = DEFAULT.copyOf()
      .withBlockBasedInterlining(BLOCK_BASED_INTERLINEING)
      .withDiscardMinTransferTimes(DISCARD_MIN_TRANSFERTIMES)
      .withMaxInterlineDistance(MAX_INTERLINE_DISTANCE)
      .withRemoveRepeatedStops(REMOVE_REPEATED_STOPS)
      .withStationTransferPreference(STATION_TRANSFER_PREFERENCE)
      .build();

    SUBJECT_FEED = SUBJECT_DEFAULT.withFeedInfo().withFeedId(FEED).withSource(SOURCE).build();

    SUBJECT_FEED_WITHOUT_FEED_ID = SUBJECT_DEFAULT.withFeedInfo().withSource(SOURCE).build();
  }

  @Test
  void removeRepeatedStops() {
    assertEquals(REMOVE_REPEATED_STOPS, SUBJECT_DEFAULT.removeRepeatedStops());
    assertEquals(REMOVE_REPEATED_STOPS, SUBJECT_FEED.removeRepeatedStops());
  }

  @Test
  void stationTransferPreference() {
    assertEquals(STATION_TRANSFER_PREFERENCE, SUBJECT_DEFAULT.stationTransferPreference());
    assertEquals(STATION_TRANSFER_PREFERENCE, SUBJECT_FEED.stationTransferPreference());
  }

  @Test
  void discardMinTransferTimes() {
    assertEquals(DISCARD_MIN_TRANSFERTIMES, SUBJECT_DEFAULT.discardMinTransferTimes());
    assertEquals(DISCARD_MIN_TRANSFERTIMES, SUBJECT_FEED.discardMinTransferTimes());
  }

  @Test
  void blockBasedInterlining() {
    assertEquals(BLOCK_BASED_INTERLINEING, SUBJECT_DEFAULT.blockBasedInterlining());
    assertEquals(BLOCK_BASED_INTERLINEING, SUBJECT_FEED.blockBasedInterlining());
  }

  @Test
  void maxInterlineDistance() {
    assertEquals(MAX_INTERLINE_DISTANCE, SUBJECT_DEFAULT.maxInterlineDistance());
    assertEquals(MAX_INTERLINE_DISTANCE, SUBJECT_FEED.maxInterlineDistance());
  }

  @Test
  void feedInfo() {
    assertEquals(FEED, SUBJECT_FEED.feedId());
    assertNull(SUBJECT_FEED_WITHOUT_FEED_ID.feedId());
  }

  @Test
  void source() {
    assertEquals(SOURCE, SUBJECT_FEED.source());
  }

  @Test
  void copyOf() {
    assertEquals("GtfsDefaultParameters{}", DEFAULT.copyOf().build().toString());
    assertEquals(EXPECTED_DEFAULT_TO_STRING, SUBJECT_DEFAULT.copyOf().build().toString());
    assertEquals(EXPECTED_DEFAULT_TO_STRING, SUBJECT_FEED.copyOf().build().toString());
  }

  @Test
  void withFeedInfo() {
    assertEquals(
      EXPECTED_FEED_TO_STRING,
      SUBJECT_DEFAULT.withFeedInfo().withFeedId(FEED).withSource(SOURCE).build().toString()
    );
  }

  @Test
  void testEqualsAndHashCode() {
    assertThrows(UnsupportedOperationException.class, () -> SUBJECT_DEFAULT.equals("A"));
    assertThrows(UnsupportedOperationException.class, () -> SUBJECT_DEFAULT.hashCode());
  }

  @Test
  void testToString() {
    assertEquals(EXPECTED_DEFAULT_TO_STRING, SUBJECT_DEFAULT.toString());
    assertEquals(EXPECTED_FEED_TO_STRING, SUBJECT_FEED.toString());
    assertEquals(EXPECTED_FEED_WITHOUT_FEED_ID_TO_STRING, SUBJECT_FEED_WITHOUT_FEED_ID.toString());
  }
}
