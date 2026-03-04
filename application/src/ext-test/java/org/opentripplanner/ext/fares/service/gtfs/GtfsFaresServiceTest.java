package org.opentripplanner.ext.fares.service.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimitType;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.ext.fares.service.gtfs.v2.GtfsFaresV2Service;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;

class GtfsFaresServiceTest implements FareTestConstants {

  private final GtfsFaresV2Service V2_SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id(1), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP_A)
        .withNetworkId(NETWORK_A.getId())
        .build(),
      FareLegRule.of(id(2), FARE_PRODUCT_B)
        .withLegGroupId(LEG_GROUP_B)
        .withNetworkId(NETWORK_A.getId())
        .build()
    )
    .withTransferRules(
      FareTransferRule.of()
        .withId(id(3))
        .withFareProducts()
        .withFromLegGroup(LEG_GROUP_A)
        .withToLegGroup(LEG_GROUP_A)
        .withTimeLimit(TimeLimitType.DEPARTURE_TO_ARRIVAL, Duration.ofDays(1))
        .build()
    )
    .build();

  @Test
  void twoLegsWithSingleAndDailyPass() {
    var leg1 = TestTransitLeg.of()
      .withStartTime("10:00")
      .withEndTime("10:30")
      .withNetwork(NETWORK_A.getId())
      .build();
    var leg2 = TestTransitLeg.of()
      .withStartTime("10:40")
      .withEndTime("11:00")
      .withNetwork(NETWORK_A.getId())
      .build();

    var service = new GtfsFaresService(new DefaultFareService(), V2_SERVICE);

    var result = service.calculateFares(TestItinerary.of(leg1, leg2).build());

    assertThat(result.getLegProducts().get(leg1)).containsExactly(
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_A),
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_B)
    );

    assertThat(result.getLegProducts().get(leg2)).containsExactly(
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_A),
      FareOffer.of(leg2.startTime(), FARE_PRODUCT_B)
    );
  }
}
