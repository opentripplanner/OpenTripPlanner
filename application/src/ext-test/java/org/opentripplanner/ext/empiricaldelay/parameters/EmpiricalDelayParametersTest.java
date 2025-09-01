package org.opentripplanner.ext.empiricaldelay.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner._support.net.URIUtils;

class EmpiricalDelayParametersTest {

  private static final String FEED_ID = "FEED_ID";

  private static final URI SOURCE = URIUtils.uri("file://test");
  private static final EmpiricalDelayFeedParameters FEED_PARAMS = new EmpiricalDelayFeedParameters(
    FEED_ID,
    SOURCE
  );

  private final EmpiricalDelayParameters subject = EmpiricalDelayParameters.of()
    .addFeeds(List.of(FEED_PARAMS))
    .build();

  @Test
  void listFiles() {
    assertEquals(List.of(SOURCE), subject.listFiles());
  }

  @Test
  void feeds() {
    assertEquals(List.of(FEED_PARAMS), subject.feeds());
  }

  @Test
  void testEqualsAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(new EmpiricalDelayParameters(List.of(FEED_PARAMS)))
      .differentFrom(
        EmpiricalDelayParameters.DEFAULT,
        new EmpiricalDelayParameters(
          List.of(
            FEED_PARAMS,
            new EmpiricalDelayFeedParameters(FEED_ID, URIUtils.uri("file://test2"))
          )
        )
      );
  }

  @Test
  void testToString() {
    assertEquals(
      "EmpiricalDelayParameters{fedds: [EmpiricalDelayFeedParameters{feedId: 'FEED_ID', source: file://test}]}",
      subject.toString()
    );
  }

  @Test
  void isSerializable() {
    assertTrue(subject instanceof Serializable);
  }
}
