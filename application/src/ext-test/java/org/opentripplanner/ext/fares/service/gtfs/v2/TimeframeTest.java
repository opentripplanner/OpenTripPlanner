package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItinerary;
import org.opentripplanner.model.plan.TestTransitLeg;

class TimeframeTest implements PlanTestConstants, FareTestConstants {

  @Test
  void oneLeg() {
    var leg = TestTransitLeg.of()
      .withStartTime("12:00")
      .withEndTime("12:10")
      .withServiceId(TIMEFRAME_TWELVE_TO_TWO.serviceId())
      .build();
    var it = TestItinerary.of(leg).build();
    var result = buildService(leg.serviceDate()).calculateFares(it);

    assertThat(result.itineraryProducts()).isEmpty();
    assertThat(result.offersForLeg(leg)).containsExactly(
      FareOffer.of(leg.startTime(), FARE_PRODUCT_A)
    );
  }

  @Test
  void twoLegs() {
    var leg1 = TestTransitLeg.of()
      .withStartTime("12:00")
      .withEndTime("12:10")
      .withServiceId(TIMEFRAME_TWELVE_TO_TWO.serviceId())
      .build();
    var leg2 = TestTransitLeg.of()
      .withStartTime("15:20")
      .withEndTime("15:30")
      .withServiceId(TIMEFRAME_THREE_TO_FIVE.serviceId())
      .build();
    var it = TestItinerary.of(leg1, leg2).build();
    var result = buildService(leg1.serviceDate()).calculateFares(it);

    assertThat(result.itineraryProducts()).isEmpty();
    assertThat(result.offersForLeg(leg1)).containsExactly(
      FareOffer.of(leg1.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(leg2)).containsExactly(
      FareOffer.of(leg2.startTime(), FARE_PRODUCT_B)
    );
  }

  private static GtfsFaresV2Service buildService(LocalDate serviceDate) {
    return GtfsFaresV2Service.of()
      .withLegRules(
        FareLegRule.of(id("r1"), FARE_PRODUCT_A)
          .withLegGroupId(id("r1"))
          .withFromTimeframes(List.of(TIMEFRAME_TWELVE_TO_TWO))
          .build(),
        FareLegRule.of(id("r2"), FARE_PRODUCT_B)
          .withLegGroupId(id("r2"))
          .withFromTimeframes(List.of(TIMEFRAME_THREE_TO_FIVE))
          .build()
      )
      .addServiceId(TIMEFRAME_THREE_TO_FIVE.serviceId(), serviceDate)
      .addServiceId(TIMEFRAME_TWELVE_TO_TWO.serviceId(), serviceDate)
      .build();
  }
}
