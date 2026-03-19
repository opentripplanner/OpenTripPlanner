package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimitType;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;

class WildcardNetworkTransferTest implements FareTestConstants {

  private final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      // wildcard rule, matches everything
      FareLegRule.of(id(1), FARE_PRODUCT_A).withLegGroupId(LEG_GROUP_A).build()
    )
    .withTransferRules(
      // monthly pass with unlimited transfers
      FareTransferRule.of()
        .withId(id(3))
        .withFromLegGroup(LEG_GROUP_A)
        .withToLegGroup(LEG_GROUP_A)
        .withTimeLimit(TimeLimitType.DEPARTURE_TO_ARRIVAL, Duration.ofDays(30))
        .build()
    )
    .build();

  @Test
  void differentNetwork() {
    var leg1 = TestTransitLeg.of().withStartTime("10:00").withEndTime("10:10").build();
    var leg2 = TestTransitLeg.of().withStartTime("10:20").withEndTime("10:30").build();
    var result = SERVICE.calculateFares(TestItinerary.of(leg1, leg2).build());

    assertThat(result.offersForLeg(leg1)).containsExactly(
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_A)
    );

    assertThat(result.offersForLeg(leg2)).containsExactly(
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_A)
    );
  }
}
