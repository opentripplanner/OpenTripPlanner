package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.ImmutableMultimap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareModelForTest;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;

class GtfsFaresServiceTest implements PlanTestConstants {

  private static final FeedScopedId NETWORK = TimetableRepositoryForTest.id("network");
  private static final Route ROUTE = TimetableRepositoryForTest.route("r1")
    .withGroupOfRoutes(List.of(GroupOfRoutes.of(NETWORK).build()))
    .build();
  private static final Itinerary ITIN = TestItineraryBuilder.newItinerary(A)
    .bus(ROUTE, 1, 0, 50, B)
    .build();
  private static final Agency AGENCY = ITIN.transitLeg(0).agency();
  public static final Money V1_PRICE = Money.usDollars(1);
  public static final Money V2_PRICE = FareModelForTest.FARE_PRODUCT_A.price();
  private static final FareRuleSet FRS = new FareRuleSet(
    FareAttribute.of(id("1")).setPrice(V1_PRICE).setAgency(AGENCY.getId()).build()
  );
  private static final FareLegRule LEG_RULE = FareLegRule.of(
    id("2"),
    List.of(FareModelForTest.FARE_PRODUCT_A)
  ).build();

  @Test
  void combineV1andV2() {
    var v1 = new DefaultFareService();

    v1.addFareRules(FareType.regular, List.of(FRS));

    var v2 = new GtfsFaresV2Service(List.of(LEG_RULE), List.of(), ImmutableMultimap.of());
    var service = new GtfsFaresService(v1, v2);

    var fare = service.calculateFares(ITIN);
    assertThat(fare.getLegProducts()).hasSize(2);

    var prices = fare
      .getLegProducts()
      .values()
      .stream()
      .map(fp -> fp.fareProduct().price())
      .toList();
    assertThat(prices).containsExactly(V1_PRICE, V2_PRICE);
  }
}
