package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model.network.GroupOfRoutes;

class StopCountNetworkTest implements PlanTestConstants, FareTestConstants {

  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("nA"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP_A)
        .withNetworkId(NETWORK_A.getId())
        .withFareDistance(new FareDistance.Stops(0, 2))
        .build(),
      FareLegRule.of(id("nB"), FARE_PRODUCT_B)
        .withLegGroupId(LEG_GROUP_A)
        .withNetworkId(NETWORK_B.getId())
        .withFareDistance(new FareDistance.Stops(0, 2))
        .build()
    )
    .build();
  private static final TransitLeg TWO_STOPS_ON_NETWORK_A = leg(NETWORK_A);
  private static final TransitLeg TWO_STOPS_ON_NETWORK_B = leg(NETWORK_B);

  @Test
  void networkA() {
    var result = SERVICE.calculateFares(TestItinerary.of(TWO_STOPS_ON_NETWORK_A).build());
    assertThat(result.offersForLeg(TWO_STOPS_ON_NETWORK_A)).containsExactly(
      FareOffer.of(TWO_STOPS_ON_NETWORK_A.startTime(), FARE_PRODUCT_A)
    );
  }

  @Test
  void networkB() {
    var result = SERVICE.calculateFares(TestItinerary.of(TWO_STOPS_ON_NETWORK_B).build());
    assertThat(result.offersForLeg(TWO_STOPS_ON_NETWORK_B)).containsExactly(
      FareOffer.of(TWO_STOPS_ON_NETWORK_B.startTime(), FARE_PRODUCT_B)
    );
  }

  private static TestTransitLeg leg(GroupOfRoutes network) {
    return TestTransitLeg.of()
      .withStartTime("10:00")
      .withEndTime("11:00")
      .withNetwork(network)
      .withIntermediateStops(id(1))
      .build();
  }
}
