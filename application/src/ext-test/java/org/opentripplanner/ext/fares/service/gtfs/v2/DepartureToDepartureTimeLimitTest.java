package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimitType;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;

class DepartureToDepartureTimeLimitTest implements PlanTestConstants, FareTestConstants {

  private static final FeedScopedId LEG_GROUP = id("leg-group-a");

  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(FareLegRule.of(id("r1"), FARE_PRODUCT_A).withLegGroupId(LEG_GROUP).build())
    .withTransferRules(
      List.of(
        FareTransferRule.of()
          .withId(id("transfer"))
          .withFromLegGroup(LEG_GROUP)
          .withToLegGroup(LEG_GROUP)
          .withTransferCount(FareTransferRule.UNLIMITED_TRANSFERS)
          .withFareProducts(List.of(TRANSFER_1))
          .withTimeLimit(TimeLimitType.DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(10))
          .build()
      )
    )
    .build();

  @Test
  void twoLegs() {
    var i1 = newItinerary(A, time("10:00"))
      .bus(1, time("10:00"), time("10:08"), B)
      .bus(2, time("10:09"), time("10:15"), C)
      .build();

    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();

    var first = i1.legs().getFirst();
    var last = i1.legs().getLast();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(last.startTime(), FARE_PRODUCT_A),
      FareOffer.of(first.startTime(), TRANSFER_1, List.of(FARE_PRODUCT_A))
    );
  }

  @Test
  void twoLegsAboveLimit() {
    var i1 = newItinerary(A, time("10:00"))
      .bus(1, time("10:00"), time("10:04"), B)
      .bus(2, time("10:11"), time("10:15"), C)
      .build();

    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();

    var first = i1.legs().getFirst();
    var last = i1.legs().getLast();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(last.startTime(), FARE_PRODUCT_A)
    );
  }
}
