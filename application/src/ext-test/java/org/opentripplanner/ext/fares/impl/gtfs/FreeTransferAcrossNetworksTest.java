package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

class FreeTransferAcrossNetworksTest implements PlanTestConstants, FareTestConstants {

  private static final Route ROUTE_A = TimetableRepositoryForTest.route("A")
    .withGroupOfRoutes(List.of(NETWORK_A))
    .build();
  private static final Route ROUTE_B = TimetableRepositoryForTest.route("B")
    .withGroupOfRoutes(List.of(NETWORK_B))
    .build();

  GtfsFaresV2Service service = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(LEG_GROUP_A, FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP_A)
        .withNetworkId(NETWORK_A.getId())
        .build(),
      FareLegRule.of(LEG_GROUP_B, FARE_PRODUCT_B)
        .withLegGroupId(LEG_GROUP_B)
        .withNetworkId(NETWORK_B.getId())
        .build()
    ),
    List.of(
      FareTransferRule.of()
        .withId(id("transfer"))
        .withFromLegGroup(LEG_GROUP_A)
        .withToLegGroup(LEG_GROUP_B)
        .build()
    ),
    Multimaps.forMap(Map.of())
  );

  @Test
  void freeTransferAcrossNetwork() {
    var itin = newItinerary(A, 0).bus(ROUTE_A, 1, 0, 10, B).bus(ROUTE_B, 1, 15, 25, B).build();
    var result = service.calculateFares(itin);
    var first = itin.legs().getFirst();
    var second = itin.legs().getLast();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.itineraryProducts()).containsExactly(FARE_PRODUCT_A);
  }
}
