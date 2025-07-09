package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.fares.model.FareTransferRule.UNLIMITED_TRANSFERS;
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
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

class CostedTransferInNetworkTest implements PlanTestConstants, FareTestConstants {

  private static final Route ROUTE_1 = routeInNetwork("r1");
  private static final Route ROUTE_2 = routeInNetwork("r2");
  private static final Route ROUTE_3 = routeInNetwork("r3");
  private static final Route ROUTE_4 = TimetableRepositoryForTest.route("r4").build();
  private static final FeedScopedId LEG_GROUP = id("leg-group-a");

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("r1"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK_A.getId())
        .build(),
      FareLegRule.of(id("r2"), FARE_PRODUCT_B)
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
        .withFareProducts(List.of(TRANSFER_1))
        .build()
    ),
    Multimaps.forMap(Map.of())
  );

  @Test
  void twoLegs() {
    var i1 = newItinerary(A, 0).bus(ROUTE_1, 1, 0, 20, B).bus(ROUTE_2, 2, 21, 40, C).build();

    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();
    var first = i1.legs().getFirst();
    var last = i1.legs().getLast();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A),
      FareOffer.of(first.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A, FARE_PRODUCT_B)),
      FareOffer.of(last.startTime(), FARE_PRODUCT_A),
      FareOffer.of(last.startTime(), FARE_PRODUCT_B)
    );
  }

  /**
   * Tests that a transfer product is correctly applied to three consecutive legs of the same network:
   * Transfers are one dollar but unlimited, so you need to either product a or b plus a single
   * transfer for the last two legs.
   */
  @Test
  void threeLegs() {
    var i1 = newItinerary(A, 0)
      .bus(ROUTE_1, 1, 0, 20, B)
      .bus(ROUTE_2, 2, 21, 40, C)
      .bus(ROUTE_3, 3, 41, 45, D)
      .build();

    var result = SERVICE.calculateFares(i1);

    var first = i1.legs().getFirst();
    var second = i1.legs().get(1);
    var last = i1.legs().getLast();

    assertThat(result.itineraryProducts()).isEmpty();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A),
      FareOffer.of(first.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(second)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A, FARE_PRODUCT_B)),
      FareOffer.of(second.startTime(), FARE_PRODUCT_A),
      FareOffer.of(second.startTime(), FARE_PRODUCT_B)
    );
    assertThat(result.offersForLeg(last)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A, FARE_PRODUCT_B)),
      FareOffer.of(last.startTime(), FARE_PRODUCT_A),
      FareOffer.of(last.startTime(), FARE_PRODUCT_B)
    );
  }

  @Test
  void threeLegsDifferentNetwork() {
    var i1 = newItinerary(A, 0)
      .bus(ROUTE_1, 1, 0, 20, B)
      .bus(ROUTE_2, 2, 21, 40, C)
      .bus(ROUTE_4, 3, 41, 45, D)
      .build();

    var result = SERVICE.calculateFares(i1);

    assertEquals(Set.of(), result.itineraryProducts());
    var first = i1.legs().getFirst();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A),
      FareOffer.of(first.startTime(), FARE_PRODUCT_B)
    );
    var secondLeg = i1.legs().get(1);
    assertThat(result.offersForLeg(secondLeg)).containsExactly(
      FareOffer.of(first.startTime(), TRANSFER_1, Set.of(FARE_PRODUCT_A, FARE_PRODUCT_B)),
      FareOffer.of(secondLeg.startTime(), FARE_PRODUCT_A),
      FareOffer.of(secondLeg.startTime(), FARE_PRODUCT_B)
    );
    assertEquals(Set.of(), result.offersForLeg(i1.legs().getLast()));
  }

  private static Route routeInNetwork(String id) {
    return TimetableRepositoryForTest.route(id).withGroupOfRoutes(List.of(NETWORK_A)).build();
  }
}
