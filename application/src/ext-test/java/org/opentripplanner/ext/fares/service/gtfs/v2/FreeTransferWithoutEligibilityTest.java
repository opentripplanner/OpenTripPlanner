package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;

class FreeTransferWithoutEligibilityTest implements PlanTestConstants, FareTestConstants {

  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("adult"), FARE_PRODUCT_A).withLegGroupId(LEG_GROUP_A).build(),
      FareLegRule.of(id("youth"), FARE_PRODUCT_B).withLegGroupId(LEG_GROUP_A).build()
    )
    .withTransferRules(
      FareTransferRule.of()
        .withId(id(1))
        .withFromLegGroup(LEG_GROUP_A)
        .withToLegGroup(LEG_GROUP_A)
        .withFareProducts(List.of())
        .build()
    )
    .build();

  @Test
  void freeTransfer() {
    var firstLeg = TestTransitLeg.of().withStartTime("10:00").withEndTime("11:00").build();
    var secondLeg = TestTransitLeg.of().withStartTime("11:00").withEndTime("12:00").build();

    var result = SERVICE.calculateFares(TestItinerary.of(firstLeg, secondLeg).build());

    assertThat(result.offersForLeg(firstLeg)).containsExactly(
      FareOffer.of(firstLeg.startTime(), FARE_PRODUCT_A),
      FareOffer.of(firstLeg.startTime(), FARE_PRODUCT_B)
    );

    assertThat(result.offersForLeg(secondLeg)).containsExactly(
      FareOffer.of(firstLeg.startTime(), FARE_PRODUCT_A),
      FareOffer.of(firstLeg.startTime(), FARE_PRODUCT_B)
    );
  }
}
