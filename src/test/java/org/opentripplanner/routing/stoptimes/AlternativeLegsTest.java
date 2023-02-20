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

  @Override
  public String getFeedName() {
    return "testagency";
  }

  @Test
  void testPreviousLegs() throws Exception {
    var transitService = new DefaultTransitService(transitModel);

    var originalLeg = new ScheduledTransitLegReference(
      new FeedScopedId(this.feedId.getId(), "1.2"),
      LocalDate.parse("2022-04-02"),
      1,
      2
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

    var expectd = String.join(
      ", ",
      List.of(
        "B ~ BUS 2 0:20 0:30 ~ C [$-1]",
        "B ~ BUS 1 0:10 0:20 ~ C [$-1]",
        "B ~ BUS 1 8:20 8:30 ~ C [$-1]" // Previous day
      )
    );

    assertEquals(expectd, legs);
  }

  @Test
  void testNextLegs() throws Exception {
    var transitService = new DefaultTransitService(transitModel);

    var originalLeg = new ScheduledTransitLegReference(
      new FeedScopedId(this.feedId.getId(), "2.2"),
      LocalDate.parse("2022-04-02"),
      0,
      1
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

    var expectd = String.join(
      ", ",
      List.of(
        "B ~ BUS 3 1:00 1:10 ~ C [$-1]",
        "B ~ BUS 1 8:20 8:30 ~ C [$-1]",
        "B ~ BUS 1 0:10 0:20 ~ C [$-1]" // Next day
      )
    );

    assertEquals(expectd, legs);
  }
}
