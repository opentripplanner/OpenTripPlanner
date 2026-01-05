package org.opentripplanner.ext.fares.service.gtfs.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.groupOfRoutes;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;

class FreeTransferInNetworkTest implements PlanTestConstants {

  private static final GroupOfRoutes NETWORK = groupOfRoutes("n1").build();
  private static final Route ROUTE = TimetableRepositoryForTest.route("r1")
    .withGroupOfRoutes(List.of(NETWORK))
    .build();
  private static final FeedScopedId LEG_GROUP = id("leg-group1");

  private static final FareProduct REGULAR = FareProduct.of(
    id("regular"),
    "regular",
    Money.euros(5)
  ).build();

  GtfsFaresV2Service service = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("6"), REGULAR)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK.getId())
        .build()
    )
    .withTransferRules(
      List.of(
        FareTransferRule.of()
          .withId(id("transfer"))
          .withFromLegGroup(LEG_GROUP)
          .withToLegGroup(LEG_GROUP)
          .build()
      )
    )
    .build();

  @Test
  void differentNetwork() {
    var i1 = newItinerary(A, 0).bus(1, 0, 50, B).build();
    var result = service.calculateFares(i1);
    assertEquals(Set.of(), result.itineraryProducts());
  }

  @Test
  void singleLeg() {
    var i1 = newItinerary(A, 0).bus(ROUTE, 1, 0, 50, B).build();
    var result = service.calculateFares(i1);
    assertEquals(
      Set.of(FareOffer.of(i1.startTime(), REGULAR)),
      result.offersForLeg(i1.legs().getFirst())
    );
  }

  @Test
  void severalLegs() {
    var i1 = newItinerary(A, 0).bus(ROUTE, 1, 0, 50, B).bus(ROUTE, 1, 0, 50, C).build();
    var result = service.calculateFares(i1);
    assertEquals(Set.of(REGULAR), result.itineraryProducts());
  }
}
