package org.opentripplanner.ext.empiricaldelay.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.net.URIUtils;

class EmpiricalDelayFeedParametersTest {

  private static final String FEED_ID = "FEED_ID";
  private static final URI SOURCE = URIUtils.uri("file://test");

  private final EmpiricalDelayFeedParameters subject = new EmpiricalDelayFeedParameters(
    FEED_ID,
    SOURCE
  );

  @Test
  void feedId() {
    assertEquals(FEED_ID, subject.feedId());
  }

  @Test
  void source() {
    assertEquals(SOURCE, subject.source());
  }

  @Test
  void testToString() {
    assertEquals(
      "EmpiricalDelayFeedParameters{feedId: 'FEED_ID', source: file://test}",
      subject.toString()
    );
  }

  @Test
  void isSerializable() {
    assertTrue(subject instanceof Serializable);
  }
}
