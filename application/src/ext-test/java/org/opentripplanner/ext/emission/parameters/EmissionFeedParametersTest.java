package org.opentripplanner.ext.emission.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner._support.net.URIUtils;

class EmissionFeedParametersTest {

  private static final String FEED_ID = "my_feed_id";
  private static final String OTHER_FEED_ID = "other_feed_id";
  private static final URI SOURCE = URIUtils.uri("http://host/emsissions");
  private static final URI OTHER_SOURCE = URIUtils.uri("http://host/accids");

  private final EmissionFeedParameters subject = new EmissionFeedParameters(FEED_ID, SOURCE);

  @Test
  void testToString() {
    assertEquals(
      "EmissionFeedParameters{feedId: 'my_feed_id', source: http://host/emsissions}",
      subject.toString()
    );
  }

  @Test
  void feedId() {
    assertEquals(FEED_ID, subject.feedId());
  }

  @Test
  void source() {
    assertEquals(SOURCE, subject.source());
  }

  @Test
  void testEqualsAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(new EmissionFeedParameters(FEED_ID, SOURCE))
      .differentFrom(
        new EmissionFeedParameters(OTHER_FEED_ID, SOURCE),
        new EmissionFeedParameters(FEED_ID, OTHER_SOURCE)
      );
  }
}
