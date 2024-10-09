package org.opentripplanner.routing.stoptimes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.alternativelegs.AlternativeLegs;
import org.opentripplanner.routing.alternativelegs.AlternativeLegsFilter;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.DefaultTransitService;

/**
 * Check that the correct alternative legs are found, and that the search traverses the date
 * boundary correctly
 */
class AlternativeLegsTest extends GtfsTest {

  private static final String FEED_ID = "FEED";
  private static final FeedScopedId STOP_ID_B = new FeedScopedId(FEED_ID, "B");
  private static final FeedScopedId STOP_ID_C = new FeedScopedId(FEED_ID, "C");
  private static final FeedScopedId STOP_ID_X = new FeedScopedId(FEED_ID, "X");
  private static final FeedScopedId STOP_ID_Y = new FeedScopedId(FEED_ID, "Y");

  @Override
  public String getFeedName() {
    return "gtfs/simple";
  }

  @Test
  void testPreviousLegs() {
    var transitService = new DefaultTransitService(transitModel);

    var originalLeg = new ScheduledTransitLegReference(
      new FeedScopedId(this.feedId.getId(), "1.2"),
      LocalDate.parse("2022-04-02"),
      1,
      2,
      STOP_ID_B,
      STOP_ID_C,
      null
    )
      .getLeg(transitService);

    final List<ScheduledTransitLeg> alternativeLegs = AlternativeLegs.getAlternativeLegs(
      originalLeg,
      3,
      transitService,
      true,
      AlternativeLegsFilter.NO_FILTER
    );

    var legs = Itinerary.toStr(
      alternativeLegs.stream().map(Leg.class::cast).map(List::of).map(Itinerary::new).toList()
    );

    var expected =
      "B ~ BUS 2 0:20 0:30 ~ C [C₁-1], " +
      "B ~ BUS 1 0:10 0:20 ~ C [C₁-1], " +
      // Previous day
      "B ~ BUS 1 8:20 8:30 ~ C [C₁-1]";

    assertEquals(expected, legs);
  }

  @Test
  void testNextLegs() {
    var transitService = new DefaultTransitService(transitModel);

    var originalLeg = new ScheduledTransitLegReference(
      new FeedScopedId(this.feedId.getId(), "2.2"),
      LocalDate.parse("2022-04-02"),
      0,
      1,
      STOP_ID_B,
      STOP_ID_C,
      null
    )
      .getLeg(transitService);

    final List<ScheduledTransitLeg> alternativeLegs = AlternativeLegs.getAlternativeLegs(
      originalLeg,
      3,
      transitService,
      false,
      AlternativeLegsFilter.NO_FILTER
    );

    var legs = Itinerary.toStr(
      alternativeLegs.stream().map(Leg.class::cast).map(List::of).map(Itinerary::new).toList()
    );

    var expected =
      "B ~ BUS 3 1:00 1:10 ~ C [C₁-1], " +
      "B ~ BUS 1 8:20 8:30 ~ C [C₁-1], " +
      // Next day
      "B ~ BUS 1 0:10 0:20 ~ C [C₁-1]";

    assertEquals(expected, legs);
  }

  @Test
  void testCircularRoutes() {
    var transitService = new DefaultTransitService(transitModel);

    var originalLeg = new ScheduledTransitLegReference(
      new FeedScopedId(this.feedId.getId(), "19.1"),
      LocalDate.parse("2022-04-02"),
      1,
      2,
      STOP_ID_X,
      STOP_ID_Y,
      null
    )
      .getLeg(transitService);

    final List<ScheduledTransitLeg> alternativeLegs = AlternativeLegs.getAlternativeLegs(
      originalLeg,
      2,
      transitService,
      false,
      AlternativeLegsFilter.NO_FILTER
    );
    var legs = Itinerary.toStr(
      alternativeLegs.stream().map(Leg.class::cast).map(List::of).map(Itinerary::new).toList()
    );

    assertEquals("X ~ BUS 19 10:30 10:40 ~ Y [C₁-1], X ~ BUS 19 10:00 10:10 ~ Y [C₁-1]", legs);
  }

  @Test
  void testComplexCircularRoutes() {
    var transitService = new DefaultTransitService(transitModel);

    var originalLeg = new ScheduledTransitLegReference(
      new FeedScopedId(this.feedId.getId(), "19.1"),
      LocalDate.parse("2022-04-02"),
      1,
      7,
      STOP_ID_X,
      STOP_ID_B,
      null
    )
      .getLeg(transitService);

    final List<ScheduledTransitLeg> alternativeLegs = AlternativeLegs.getAlternativeLegs(
      originalLeg,
      2,
      transitService,
      false,
      AlternativeLegsFilter.NO_FILTER
    );
    var legs = Itinerary.toStr(
      alternativeLegs.stream().map(Leg.class::cast).map(List::of).map(Itinerary::new).toList()
    );

    var expected = String.join(", ", List.of("X ~ BUS 19 10:30 11:00 ~ B [C₁-1]"));
    assertEquals(expected, legs);
  }
}
