package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimitType;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.model.plan.TransitLeg;

class TransferCountWithTimeLimitTest implements PlanTestConstants, FareTestConstants {

  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("r1"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP_A)
        .withNetworkId(NETWORK_A.getId())
        .build()
    )
    .withTransferRules(
      List.of(
        FareTransferRule.of()
          .withId(id("transfer"))
          .withFromLegGroup(LEG_GROUP_A)
          .withToLegGroup(LEG_GROUP_A)
          .withTimeLimit(TimeLimitType.DEPARTURE_TO_ARRIVAL, Duration.ofHours(2))
          .withTransferCount(1)
          .build()
      )
    )
    .build();

  private static final TransitLeg LEG_1 = TestTransitLeg.of()
    .withStartTime("10:00")
    .withEndTime("10:10")
    .withNetwork(NETWORK_A.getId())
    .build();
  private static final TransitLeg LEG_2 = TestTransitLeg.of()
    .withStartTime("10:20")
    .withEndTime("10:30")
    .withNetwork(NETWORK_A.getId())
    .build();
  private static final TransitLeg LEG_3 = TestTransitLeg.of()
    .withStartTime("10:40")
    .withEndTime("10:50")
    .withNetwork(NETWORK_A.getId())
    .build();
  private static final TransitLeg LEG_4 = TestTransitLeg.of()
    .withStartTime("10:50")
    .withEndTime("10:55")
    .withNetwork(NETWORK_A.getId())
    .build();

  @Test
  void twoTransfers() {
    var result = SERVICE.calculateFares(TestItinerary.of(LEG_1, LEG_2, LEG_3).build());

    assertThat(result.itineraryProducts()).isEmpty();

    assertThat(result.offersForLeg(LEG_1)).containsExactly(
      FareOffer.of(LEG_1.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(LEG_2)).containsExactly(
      FareOffer.of(LEG_1.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(LEG_3)).containsExactly(
      FareOffer.of(LEG_3.startTime(), FARE_PRODUCT_A)
    );
  }

  @Test
  void threeTransfers() {
    var result = SERVICE.calculateFares(TestItinerary.of(LEG_1, LEG_2, LEG_3, LEG_4).build());

    assertThat(result.itineraryProducts()).isEmpty();

    assertThat(result.offersForLeg(LEG_1)).containsExactly(
      FareOffer.of(LEG_1.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(LEG_2)).containsExactly(
      FareOffer.of(LEG_1.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(LEG_3)).containsExactly(
      FareOffer.of(LEG_3.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(LEG_4)).containsExactly(
      FareOffer.of(LEG_3.startTime(), FARE_PRODUCT_A)
    );
  }
}
