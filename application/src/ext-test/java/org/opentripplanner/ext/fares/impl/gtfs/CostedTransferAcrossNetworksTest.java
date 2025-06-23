package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
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
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

class CostedTransferAcrossNetworksTest implements PlanTestConstants, FareTestConstants {

  private static final Route ROUTE_A = TimetableRepositoryForTest.route("A")
    .withGroupOfRoutes(List.of(NETWORK_A))
    .build();
  private static final Route ROUTE_B = TimetableRepositoryForTest.route("B")
    .withGroupOfRoutes(List.of(NETWORK_B))
    .build();

  private final GtfsFaresV2Service service = new GtfsFaresV2Service(
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

    var first = itin.legs().getFirst();
    assertThat(result.offersForLeg(first)).containsExactly(FareOffer.of(FARE_PRODUCT_A));
    var secondLeg = itin.legs().get(1);
    assertThat(result.offersForLeg(secondLeg)).containsExactly(FareOffer.of(FARE_PRODUCT_A));
    var last = itin.legs().getLast();
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(secondLeg.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(FARE_PRODUCT_B)
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

    var firstLeg = itin.legs().getFirst();
    var secondLeg = itin.legs().get(1);

    assertThat(result.offersForLeg(firstLeg)).containsExactly(FareOffer.of(FARE_PRODUCT_A));
    assertThat(result.offersForLeg(secondLeg)).containsExactly(FareOffer.of(FARE_PRODUCT_A));
    assertThat(result.offersForLeg(itin.legs().get(2))).containsExactly(
      FareOffer.of(secondLeg.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(itin.legs().getLast())).containsExactly(
      FareOffer.of(FARE_PRODUCT_A)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }

  /**
   * Taking route A, then transferring to B and then another B should lead to a single
   * dependent fare product for the second and third leg.
   */
  @Test
  void ABB() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_A, 1, 0, 10, A)
      .bus(ROUTE_B, 2, 11, 20, A)
      .bus(ROUTE_B, 3, 21, 25, A)
      .build();

    var result = service.calculateFares(itin);

    var first = itin.legs().getFirst();
    assertThat(result.offersForLeg(itin.legs().getFirst())).containsExactly(
      FareOffer.of(FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(itin.legs().get(1))).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(itin.legs().getLast())).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(FARE_PRODUCT_B)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }
}
