package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.groupOfRoutes;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl.gtfs.FareProductMatch.Transfer;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;

class TransferTest implements PlanTestConstants {

  private static final GroupOfRoutes NETWORK = groupOfRoutes("n1").build();
  private static final Route ROUTE_1 = route("r1");
  private static final Route ROUTE_2 = route("r2");
  private static final FeedScopedId LEG_GROUP = id("leg-group1");

  private static final FareProduct REGULAR = FareProduct.of(
    id("regular"),
    "regular",
    Money.euros(5)
  ).build();
  private static final FareProduct TRANSFER = FareProduct.of(
    id("transfer"),
    "transfer",
    Money.euros(1)
  ).build();

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("6"), REGULAR)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK.getId())
        .build()
    ),
    List.of(
      new FareTransferRule(id("transfer"), LEG_GROUP, LEG_GROUP, -1, null, List.of(TRANSFER))
    ),
    Multimaps.forMap(Map.of())
  );

  @Test
  void transfer() {
    var i1 = newItinerary(A, 0).bus(ROUTE_1, 1, 0, 20, B).bus(ROUTE_2, 2, 21, 40, C).build();
    var result = SERVICE.calculateFareProducts(i1);

    var leg1Products = result.match(i1.firstLeg()).get().fareProducts();
    assertEquals(Set.of(REGULAR), leg1Products);

    var leg2match = result.match(i1.lastLeg()).get();
    assertThat(leg2match.transfersFromPreviousLeg()).containsExactly(
      new Transfer(TRANSFER, List.of(REGULAR))
    );
    assertThat(leg2match.fareProducts()).containsExactly(REGULAR);
  }

  private static Route route(String id) {
    return TimetableRepositoryForTest.route(id).withGroupOfRoutes(List.of(NETWORK)).build();
  }
}
