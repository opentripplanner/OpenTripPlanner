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
      FareTransferRule.of()
        .withId(id("t1"))
        .withFromLegGroup(LEG_GROUP_A)
        .withToLegGroup(LEG_GROUP_A)
        .build(),
      // transferring from B to B is also free
      FareTransferRule.of()
        .withId(id("t2"))
        .withFromLegGroup(LEG_GROUP_B)
        .withToLegGroup(LEG_GROUP_B)
        .build(),
      // transferring from A to B costs one EUR
      FareTransferRule.of()
        .withId(id("t3"))
        .withFromLegGroup(LEG_GROUP_A)
        .withToLegGroup(LEG_GROUP_B)
        .withFareProducts(List.of(TRANSFER_1))
        .build(),
      // transferring from B to A is free
      FareTransferRule.of()
        .withId(id("t4"))
        .withFromLegGroup(LEG_GROUP_B)
        .withToLegGroup(LEG_GROUP_A)
        .build()
    ),
    Multimaps.forMap(Map.of())
  );

  @Test
  void AAB() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_A, 1, 0, 10, A)
      .bus(ROUTE_A, 2, 11, 20, A)
      .bus(ROUTE_B, 3, 21, 25, A)
      .build();

    var result = service.calculateFares(itin);

    var first = itin.legs().getFirst();
    var second = itin.legs().get(1);
    var last = itin.legs().getLast();

    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(second.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(last.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }

  @Test
  void AABA() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_A, 1, 0, 10, A)
      .bus(ROUTE_A, 2, 11, 20, A)
      .bus(ROUTE_B, 3, 21, 25, A)
      .bus(ROUTE_A, 4, 26, 30, A)
      .build();

    var result = service.calculateFares(itin);

    var first = itin.legs().getFirst();
    var second = itin.legs().get(1);
    var third = itin.legs().get(2);
    var last = itin.legs().getLast();

    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(third)).containsExactly(
      FareOffer.of(second.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(third.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(second.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(last.startTime(), FARE_PRODUCT_A),
      FareOffer.of(third.startTime(), FARE_PRODUCT_B)
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
    var second = itin.legs().get(1);
    var third = itin.legs().getLast();

    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(third)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }

  @Test
  void ABA() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_A, 1, 0, 10, B)
      .bus(ROUTE_B, 2, 11, 20, C)
      .bus(ROUTE_A, 3, 21, 25, D)
      .build();

    var result = service.calculateFares(itin);

    var first = itin.legs().getFirst();
    var second = itin.legs().get(1);
    var last = itin.legs().getLast();

    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B),
      FareOffer.of(last.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }

  @Test
  void ABAB() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_A, 1, 0, 10, B)
      .bus(ROUTE_B, 2, 11, 20, C)
      .bus(ROUTE_A, 3, 21, 25, D)
      .bus(ROUTE_B, 4, 25, 30, E)
      .build();

    var result = service.calculateFares(itin);

    var first = itin.legs().getFirst();
    var second = itin.legs().get(1);
    var third = itin.legs().get(2);
    var fourth = itin.legs().getLast();

    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(third)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B),
      FareOffer.of(third.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(fourth)).containsExactly(
      FareOffer.of(third.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(fourth.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }

  @Test
  void BAB() {
    var itin = newItinerary(A, 0)
      .bus(ROUTE_B, 1, 0, 10, B)
      .bus(ROUTE_A, 2, 11, 20, C)
      .bus(ROUTE_B, 3, 21, 25, D)
      .build();

    var result = service.calculateFares(itin);

    var first = itin.legs().getFirst();
    var second = itin.legs().get(1);
    var third = itin.legs().get(2);

    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_B),
      FareOffer.of(second.startTime(), FARE_PRODUCT_A)
    );
    assertThat(result.offersForLeg(third)).containsExactly(
      FareOffer.of(second.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A)),
      FareOffer.of(third.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.itineraryProducts()).isEmpty();
  }
}
