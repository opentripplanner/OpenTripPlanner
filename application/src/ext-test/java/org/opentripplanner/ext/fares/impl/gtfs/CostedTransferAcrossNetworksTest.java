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
import org.opentripplanner.model.fare.FareOffer.DependentFareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

class CostedTransferAcrossNetworksTest implements PlanTestConstants, FareTestConstants {

  private static final Route ROUTE_A = TimetableRepositoryForTest.route("A")
    .withGroupOfRoutes(List.of(NETWORK_A))
    .build();
  private static final Route ROUTE_B = TimetableRepositoryForTest.route("B")
    .withGroupOfRoutes(List.of(NETWORK_B))
    .build();

  private static final FareProduct REGULAR_A = fareProduct("A");
  private static final FareProduct REGULAR_B = fareProduct("B");

  private final GtfsFaresV2Service service = new GtfsFaresV2Service(
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
    List.of(
      // transferring from A to A is free
      new FareTransferRule(id("t1"), LEG_GROUP_A, LEG_GROUP_A, -1, null, List.of()),
      // transferring from A to B costs one EUR
      new FareTransferRule(id("t2"), LEG_GROUP_A, LEG_GROUP_B, -1, null, List.of(TRANSFER_1))
    ),
    Multimaps.forMap(Map.of())
  );

  @Test
  void costedTransferAcrossNetwork() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_A, 1, 0, 10, A)
      .bus(ROUTE_A, 2, 11, 20, A)
      .bus(ROUTE_B, 3, 21, 25, A)
      .build();

    var result = service.calculateFares(itin);

    assertThat(result.offersForLeg(itin.legs().getFirst())).containsExactly(
      FareOffer.of(REGULAR_A)
    );
    assertThat(result.offersForLeg(itin.legs().get(1))).containsExactly(FareOffer.of(REGULAR_A));
    assertThat(result.offersForLeg(itin.legs().getLast())).containsExactly(
      new DependentFareOffer(TRANSFER_1, Set.of(FareOffer.of(REGULAR_A))),
      FareOffer.of(REGULAR_B)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }

  @Test
  void fourLegs() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_A, 1, 0, 10, A)
      .bus(ROUTE_A, 2, 11, 20, A)
      .bus(ROUTE_B, 3, 21, 25, A)
      .bus(ROUTE_A, 4, 26, 30, A)
      .build();

    var result = service.calculateFares(itin);

    assertThat(result.offersForLeg(itin.legs().getFirst())).containsExactly(
      FareOffer.of(REGULAR_A)
    );
    assertThat(result.offersForLeg(itin.legs().get(1))).containsExactly(FareOffer.of(REGULAR_A));
    assertThat(result.offersForLeg(itin.legs().get(2))).containsExactly(
      new DependentFareOffer(TRANSFER_1, Set.of(FareOffer.of(REGULAR_A))),
      FareOffer.of(REGULAR_B)
    );
    assertThat(result.offersForLeg(itin.legs().getLast())).containsExactly(FareOffer.of(REGULAR_A));
    assertThat(result.itineraryProducts()).isEmpty();
  }
}
