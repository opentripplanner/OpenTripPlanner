package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.fares.impl._support.FareModelForTest.fareProduct;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
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

  private static final FareProduct REGULAR_A = fareProduct("A");
  private static final FareProduct REGULAR_B = fareProduct("B");

  GtfsFaresV2Service service = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(LEG_GROUP_A, REGULAR_A)
        .withLegGroupId(LEG_GROUP_A)
        .withNetworkId(NETWORK_A.getId())
        .build(),
      FareLegRule.of(LEG_GROUP_B, REGULAR_B)
        .withLegGroupId(LEG_GROUP_B)
        .withNetworkId(NETWORK_B.getId())
        .build()
    ),
    List.of(new FareTransferRule(id("transfer"), LEG_GROUP_A, LEG_GROUP_B, -1, null, List.of())),
    Multimaps.forMap(Map.of())
  );

  @Test
  void freeTransferAcrossNetwork() {
    var itin = newItinerary(A, 0).bus(ROUTE_A, 1, 0, 10, B).bus(ROUTE_B, 1, 15, 25, B).build();
    var result = service.calculateFares(itin);
    assertEquals(Set.of(FareOffer.of(REGULAR_A)), result.offersForLeg(itin.legs().getFirst()));
    assertEquals(Set.of(FareOffer.of(REGULAR_B)), result.offersForLeg(itin.legs().getLast()));
    assertThat(result.itineraryProducts()).containsExactly(REGULAR_A);
  }
}
