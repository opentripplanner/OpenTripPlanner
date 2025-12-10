package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.ext.fares.model.FareTransferRule.UNLIMITED_TRANSFERS;
import static org.opentripplanner.ext.fares.model.TimeLimitType.DEPARTURE_TO_DEPARTURE;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.utils.time.TimeUtils.time;

import com.google.common.collect.ImmutableMultimap;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.service._support.FareTestConstants;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

class FreeTransferTimeLimitTest implements PlanTestConstants, FareTestConstants {

  private static final FeedScopedId LEG_GROUP = id("leg-group-a");
  private static final Route R1 = TimetableRepositoryForTest.route("r1")
    .withGroupOfRoutes(List.of(NETWORK_A))
    .build();

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("r1"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK_A.getId())
        .build()
    ),
    List.of(
      FareTransferRule.of()
        .withId(id("transfer"))
        .withFromLegGroup(LEG_GROUP)
        .withToLegGroup(LEG_GROUP)
        .withTransferCount(UNLIMITED_TRANSFERS)
        .withTimeLimit(DEPARTURE_TO_DEPARTURE, Duration.ofMinutes(10))
        .build()
    ),
    ImmutableMultimap.of()
  );

  @Test
  void twoLegsWithinLimit() {
    var i1 = newItinerary(A, time("10:00"))
      .bus(R1, 1, time("10:00"), time("10:03"), B)
      .bus(R1, 2, time("10:04"), time("10:08"), C)
      .bus(R1, 3, time("10:12"), time("10:22"), D)
      .build();

    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();

    var first = i1.legs().getFirst();
    var second = i1.legs().get(1);
    var last = i1.legs().getLast();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(second.startTime(), FARE_PRODUCT_A)
    );
  }
}
