package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;

class TransferCountTest implements PlanTestConstants, FareTestConstants {

  private static final FeedScopedId LEG_GROUP = id("leg-group-a");

  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("r1"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK_A.getId())
        .build()
    )
    .withTransferRules(
      List.of(
        FareTransferRule.of()
          .withId(id("transfer"))
          .withFromLegGroup(LEG_GROUP)
          .withToLegGroup(LEG_GROUP)
          .withTransferCount(1)
          .build()
      )
    )
    .build();

  @Test
  void transferCount() {
    var leg1 = TestTransitLeg.of()
      .withStartTime("10:00")
      .withEndTime("10:10")
      .withNetwork(NETWORK_A.getId())
      .build();

    var leg2 = TestTransitLeg.of()
      .withStartTime("10:20")
      .withEndTime("10:30")
      .withNetwork(NETWORK_A.getId())
      .build();

    var leg3 = TestTransitLeg.of()
      .withStartTime("10:30")
      .withEndTime("10:40")
      .withNetwork(NETWORK_A.getId())
      .build();

    var i = TestItinerary.of(leg1, leg2, leg3).build();

    var result = SERVICE.calculateFares(i);

    assertThat(result.itineraryProducts()).isEmpty();

    assertThat(result.offersForLeg(leg1)).containsExactly(
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(leg2)).containsExactly(
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(leg2)).containsExactly(
      FareOffer.of(leg3.startTime(), FARE_PRODUCT_A)
    );
  }
}
