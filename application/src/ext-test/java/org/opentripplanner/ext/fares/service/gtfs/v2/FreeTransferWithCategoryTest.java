package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.transit.model.basic.Money;

/// The free transfer is only valid for the youth fare product, because it is the only one that
/// has the "youth" category. Fare product A has no category therefore doesn't qualify for the
/// free transfer.
class FreeTransferWithCategoryTest implements PlanTestConstants, FareTestConstants {

  private static final FareProduct YOUTH_PRODUCT = FareProduct.of(
    id("youth"),
    "youth",
    Money.euros(4)
  )
    .withCategory(CATEGORY_ALPHA)
    .build();
  private static final FareProduct YOUTH_TRANSFER_PRODUCT = FareProduct.of(
    id("youth-transfer"),
    "youth",
    Money.euros(0)
  )
    .withCategory(CATEGORY_ALPHA)
    .build();
  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("adult"), FARE_PRODUCT_A).withLegGroupId(LEG_GROUP_A).build(),
      FareLegRule.of(id("youth"), YOUTH_PRODUCT).withLegGroupId(LEG_GROUP_A).build()
    )
    .withTransferRules(
      FareTransferRule.of()
        .withId(id(1))
        .withFromLegGroup(LEG_GROUP_A)
        .withToLegGroup(LEG_GROUP_A)
        .withFareProducts(YOUTH_TRANSFER_PRODUCT)
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
      FareOffer.of(firstLeg.startTime(), YOUTH_PRODUCT)
    );

    assertThat(result.offersForLeg(secondLeg)).containsExactly(
      FareOffer.of(secondLeg.startTime(), FARE_PRODUCT_A),
      FareOffer.of(firstLeg.startTime(), YOUTH_PRODUCT),
      FareOffer.of(
        firstLeg.startTime(),
        YOUTH_TRANSFER_PRODUCT,
        List.of(YOUTH_PRODUCT, FARE_PRODUCT_A)
      )
    );
  }
}
