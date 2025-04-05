package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.api.request.FindStopLocationsRequest;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

public class StopLocationMatcherFactoryTest {

  private static StopLocation stop1;
  private static StopLocation stop2;

  @BeforeEach
  void setup() {
    stop1 = RegularStop.of(
      new FeedScopedId("agency", "stopId"),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("name"))
      .build();

    stop2 = RegularStop.of(
      new FeedScopedId("otherAgency", "otherStopId"),
      new AtomicInteger()::getAndIncrement
    )
      .withName(I18NString.of("otherName"))
      .build();
  }

  @Test
  public void testEmptyMatchesAll() {
    FindStopLocationsRequest request = FindStopLocationsRequest.of().build();
    Matcher<StopLocation> matcher = StopLocationMatcherFactory.of(request);
    assertTrue(matcher.match(stop1));
    assertTrue(matcher.match(stop2));
  }

  @Test
  public void testName() {
    Matcher<StopLocation> matcher = StopLocationMatcherFactory.name("name");
    assertTrue(matcher.match(stop1));
    assertFalse(matcher.match(stop2));
  }
}
