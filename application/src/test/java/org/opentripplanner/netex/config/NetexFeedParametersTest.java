package org.opentripplanner.netex.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.netex.config.NetexFeedParameters.DEFAULT;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NetexFeedParametersTest {

  private static final String FEED_ID = "FEED_ID";
  private static final String SOURCE_URI = "https://my.test.com";
  private static final String SHARED_FILE = "[sharedFil]+";
  private static final String SHARED_GROUP_FILE = "[sharedGoupFil]+";
  private static final String GROUP_FILE = "[groupFile]+";
  private static final Set<String> FERRY_IDS = Set.of("Ferry:Id");
  private static final String IGNORE_FILE = "[ignoreFl]+";

  private final NetexFeedParameters subject = NetexFeedParameters.of()
    .withFeedId(FEED_ID)
    .withSource(new URI(SOURCE_URI))
    .withSharedFilePattern(Pattern.compile(SHARED_FILE))
    .withSharedGroupFilePattern(Pattern.compile(SHARED_GROUP_FILE))
    .withGroupFilePattern(Pattern.compile(GROUP_FILE))
    .withIgnoreFilePattern(Pattern.compile(IGNORE_FILE))
    .addFerryIdsNotAllowedForBicycle(FERRY_IDS)
    .build();

  NetexFeedParametersTest() throws Exception {}

  @Test
  void feedId() {
    assertEquals(FEED_ID, subject.feedId());
  }

  @Test
  void sharedFilePattern() {
    assertEquals(SHARED_FILE, subject.sharedFilePattern().pattern());
  }

  @Test
  void sharedGroupFilePattern() {
    assertEquals(SHARED_GROUP_FILE, subject.sharedGroupFilePattern().pattern());
  }

  @Test
  void groupFilePattern() {
    assertEquals(GROUP_FILE, subject.groupFilePattern().pattern());
  }

  @Test
  void ignoreFilePattern() {
    assertEquals(IGNORE_FILE, subject.ignoreFilePattern().pattern());
  }

  @Test
  void ferryIdsNotAllowedForBicycle() {
    assertEquals(FERRY_IDS, subject.ferryIdsNotAllowedForBicycle());
  }

  @Test
  void source() {
    assertEquals(SOURCE_URI, subject.source().toASCIIString());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(NetexFeedParameters.DEFAULT, NetexFeedParameters.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withFeedId("FX").build();
    var same = other.copyOf().withFeedId(FEED_ID).build();

    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());
    assertNotEquals(subject, other);
    assertNotEquals(subject.hashCode(), other.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("NetexFeedParameters{ignoredFeatures: [PARKING]}", DEFAULT.toString());
    assertEquals(
      "NetexFeedParameters{" +
      "source: https://my.test.com, " +
      "feedId: FEED_ID, " +
      "sharedFilePattern: '[sharedFil]+', " +
      "sharedGroupFilePattern: '[sharedGoupFil]+', " +
      "groupFilePattern: '[groupFile]+', " +
      "ignoreFilePattern: '[ignoreFl]+', " +
      "ignoredFeatures: [PARKING], " +
      "ferryIdsNotAllowedForBicycle: [Ferry:Id]" +
      "}",
      subject.toString()
    );
  }
}
