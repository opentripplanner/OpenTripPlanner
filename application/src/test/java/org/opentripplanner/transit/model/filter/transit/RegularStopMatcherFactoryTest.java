package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

class RegularStopMatcherFactoryTest {

  private static RegularStop stop1;
  private static RegularStop stop2;

  @BeforeAll
  static void setup() {
    stop1 = RegularStop.of(
      new FeedScopedId("agency", "stopId"),
      new AtomicInteger()::getAndIncrement
    ).build();

    stop2 = RegularStop.of(
      new FeedScopedId("otherAgency", "otherStopId"),
      new AtomicInteger()::getAndIncrement
    ).build();
  }

  @Test
  void testFeedIds() {
    var matcher = RegularStopMatcherFactory.feedId("agency");
    assertTrue(matcher.match(stop1));
    assertFalse(matcher.match(stop2));
  }

  @Test
  void testInUseMatcher() {
    var matcher = RegularStopMatcherFactory.inUseMatcher(stop ->
      stop.getId().getFeedId().equals("agency")
    );
    assertTrue(matcher.match(stop1));
    assertFalse(matcher.match(stop2));
  }
}
