package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.fares.model.FareTransferRule.UNLIMITED_TRANSFERS;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.groupOfRoutes;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;

class CostedTransferInNetworkTest implements PlanTestConstants, FareTestConstants {

  private static final GroupOfRoutes NETWORK = groupOfRoutes("n1").build();
  private static final Route ROUTE_1 = route("r1");
  private static final Route ROUTE_2 = route("r2");
  private static final Route ROUTE_3 = TimetableRepositoryForTest.route("r3").build();
  private static final FeedScopedId LEG_GROUP = id("leg-group-a");
  private static final FareProduct ADULT_PRODUCT = FareProduct.of(
    id("adult"),
    "adult",
    Money.euros(10)
  ).build();
  private static final FareProduct YOUTH_PRODUCT = FareProduct.of(
    id("youth"),
    "youth",
    Money.euros(5)
  ).build();

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("r1"), ADULT_PRODUCT)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK.getId())
        .build(),
      FareLegRule.of(id("r2"), YOUTH_PRODUCT)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK.getId())
        .build()
    ),
    List.of(
      new FareTransferRule(
        id("transfer"),
        LEG_GROUP,
        LEG_GROUP,
        UNLIMITED_TRANSFERS,
        null,
        List.of(ONE_EUR_TRANSFER)
      )
    ),
    Multimaps.forMap(Map.of())
  );

  @Test
  void twoLegs() {
    var i1 = newItinerary(A, 0).bus(ROUTE_1, 1, 0, 20, B).bus(ROUTE_2, 2, 21, 40, C).build();

    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();
    assertThat(result.productsForLeg(i1.legs().getFirst())).containsExactly(
      new TransferFareProduct(ADULT_PRODUCT),
      new TransferFareProduct(YOUTH_PRODUCT)
    );
    assertThat(result.productsForLeg(i1.legs().getLast())).containsExactly(
      new TransferFareProduct(ONE_EUR_TRANSFER, Set.of(ADULT_PRODUCT, YOUTH_PRODUCT)),
      new TransferFareProduct(ADULT_PRODUCT),
      new TransferFareProduct(YOUTH_PRODUCT)
    );
  }

  @Test
  void threeLegs() {
    var i1 = newItinerary(A, 0)
      .bus(ROUTE_1, 1, 0, 20, B)
      .bus(ROUTE_2, 2, 21, 40, C)
      .bus(ROUTE_2, 3, 41, 45, D)
      .build();

    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();
    assertThat(result.productsForLeg(i1.legs().getFirst())).containsExactly(
      new TransferFareProduct(ADULT_PRODUCT),
      new TransferFareProduct(YOUTH_PRODUCT)
    );
    assertThat(result.productsForLeg(i1.legs().get(1))).containsExactly(
      new TransferFareProduct(ONE_EUR_TRANSFER, Set.of(ADULT_PRODUCT, YOUTH_PRODUCT)),
      new TransferFareProduct(ADULT_PRODUCT),
      new TransferFareProduct(YOUTH_PRODUCT)
    );
    assertThat(result.productsForLeg(i1.legs().getLast())).containsExactly(
      new TransferFareProduct(ONE_EUR_TRANSFER, Set.of(ADULT_PRODUCT, YOUTH_PRODUCT)),
      new TransferFareProduct(ADULT_PRODUCT),
      new TransferFareProduct(YOUTH_PRODUCT)
    );
  }

  @Test
  void threeLegsDifferentRoute() {
    var i1 = newItinerary(A, 0)
      .bus(ROUTE_1, 1, 0, 20, B)
      .bus(ROUTE_2, 2, 21, 40, C)
      .bus(ROUTE_3, 3, 41, 45, D)
      .build();

    var result = SERVICE.calculateFares(i1);

    assertEquals(Set.of(), result.itineraryProducts());
    assertThat(result.productsForLeg(i1.legs().getFirst())).containsExactly(
      new TransferFareProduct(ADULT_PRODUCT),
      new TransferFareProduct(YOUTH_PRODUCT)
    );
    assertThat(result.productsForLeg(i1.legs().get(1))).containsExactly(
      new TransferFareProduct(ONE_EUR_TRANSFER, Set.of(ADULT_PRODUCT, YOUTH_PRODUCT)),
      new TransferFareProduct(ADULT_PRODUCT),
      new TransferFareProduct(YOUTH_PRODUCT)
    );
    assertEquals(Set.of(), result.productsForLeg(i1.legs().getLast()));
  }

  private static Route route(String id) {
    return TimetableRepositoryForTest.route(id).withGroupOfRoutes(List.of(NETWORK)).build();
  }
}
