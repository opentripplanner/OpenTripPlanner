package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.model.plan.TransitLeg;

class StopCountTest implements PlanTestConstants, FareTestConstants {

  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("5-stops-max"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP_A)
        .withFareDistance(new FareDistance.Stops(0, 5))
        .build(),
      FareLegRule.of(id("4-stops-max"), FARE_PRODUCT_B)
        .withLegGroupId(LEG_GROUP_A)
        .withFareDistance(new FareDistance.Stops(0, 4))
        .build()
    )
    .build();
  private static final TransitLeg FIVE_STOPS_LEG = intermediateStops(
    id("i1"),
    id("i2"),
    id("i3"),
    id("i4")
  );
  private static final TransitLeg FOUR_STOPS_LEG = intermediateStops(id("i1"), id("i2"), id("i3"));

  private static TestTransitLeg intermediateStops(FeedScopedId... stopIds) {
    return TestTransitLeg.of()
      .withStartTime("10:00")
      .withEndTime("11:00")
      .withIntermediateStops(stopIds)
      .build();
  }

  @Test
  void fourStops() {
    var result = SERVICE.calculateFares(TestItinerary.of(FOUR_STOPS_LEG).build());
    assertThat(result.offersForLeg(FOUR_STOPS_LEG)).containsExactly(
      FareOffer.of(FOUR_STOPS_LEG.startTime(), FARE_PRODUCT_A),
      FareOffer.of(FOUR_STOPS_LEG.startTime(), FARE_PRODUCT_B)
    );
  }

  @Test
  void fiveStops() {
    var result = SERVICE.calculateFares(TestItinerary.of(FIVE_STOPS_LEG).build());
    assertThat(result.offersForLeg(FIVE_STOPS_LEG)).containsExactly(
      FareOffer.of(FIVE_STOPS_LEG.startTime(), FARE_PRODUCT_A)
    );
  }
}
