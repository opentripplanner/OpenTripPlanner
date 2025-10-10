package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;

class FareLookupServiceTest implements FareTestConstants {

  private static final FeedScopedId A_1 = id("a1");
  private static final Route ROUTE = TimetableRepositoryForTest.route("route1")
    .withGroupOfRoutes(List.of(NETWORK_A))
    .build();
  private static final ImmutableMultimap<FeedScopedId, FeedScopedId> EMPTY_STOP_AREAS =
    ImmutableMultimap.of();
  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private final RegularStop STOP_1 = testModel.stop("stop1").build();
  private final RegularStop STOP_2 = testModel.stop("stop2").build();
  private final Multimap<FeedScopedId, FeedScopedId> stopAreas = ImmutableMultimap.of(
    STOP_1.getId(),
    A_1,
    STOP_2.getId(),
    A_1
  );

  @Test
  void presenceOfAreaAndAbsenceOfPriorityLeadsToEmptyResults() {
    var r1 = FareLegRule.of(id("r1"), List.of(FARE_PRODUCT_A))
      .withNetworkId(NETWORK_A.getId())
      .build();
    var r2 = FareLegRule.of(id("r2"), List.of(FARE_PRODUCT_B))
      .withFromAreaId(A_1)
      .withToAreaId(A_1)
      .build();
    var service = new FareLookupService(List.of(r1, r2), List.of(), stopAreas);

    assertThat(service.legRules(leg())).isEmpty();
  }

  @Test
  void priorityLeadsToResults() {
    var r1 = FareLegRule.of(id("r1"), List.of(FARE_PRODUCT_A))
      .withNetworkId(NETWORK_A.getId())
      .withPriority(0)
      .build();
    var r2 = FareLegRule.of(id("r2"), List.of(FARE_PRODUCT_B))
      .withFromAreaId(A_1)
      .withToAreaId(A_1)
      .withPriority(2)
      .build();
    var service = new FareLookupService(List.of(r1, r2), List.of(), stopAreas);

    assertThat(service.legRules(leg())).containsExactly(r2);
  }

  @Test
  void networkWildCard() {
    var r1 = FareLegRule.of(id("r1"), List.of(FARE_PRODUCT_A))
      .withFromAreaId(A_1)
      .withToAreaId(A_1)
      .build();
    var r2 = FareLegRule.of(id("r2"), List.of(FARE_PRODUCT_B))
      .withFromAreaId(A_1)
      .withToAreaId(A_1)
      .build();
    var service = new FareLookupService(List.of(r1, r2), List.of(), stopAreas);

    assertThat(service.legRules(leg())).containsExactly(r1, r2);
  }

  @Test
  void conflictingRulesResolvedByPriority() {
    var r1 = FareLegRule.of(id("r1"), List.of(FARE_PRODUCT_A))
      .withNetworkId(NETWORK_A.getId())
      .withPriority(1)
      .build();
    var r2 = FareLegRule.of(id("r2"), List.of(FARE_PRODUCT_B))
      .withNetworkId(NETWORK_A.getId())
      .withPriority(2)
      .build();
    var service = new FareLookupService(List.of(r1, r2), List.of(), EMPTY_STOP_AREAS);
    assertThat(service.legRules(leg())).containsExactly(r2);
  }

  @Test
  void noPriorityReturnsBoth() {
    var r1 = FareLegRule.of(id("r1"), List.of(FARE_PRODUCT_A))
      .withNetworkId(NETWORK_A.getId())
      .build();
    var r2 = FareLegRule.of(id("r2"), List.of(FARE_PRODUCT_B))
      .withNetworkId(NETWORK_A.getId())
      .build();
    var service = new FareLookupService(List.of(r1, r2), List.of(), EMPTY_STOP_AREAS);
    assertThat(service.legRules(leg())).containsExactly(r1, r2);
  }

  @Test
  void multipleRulesWithEqualPriority() {
    var r1 = FareLegRule.of(id("r1"), List.of(FARE_PRODUCT_A))
      .withNetworkId(NETWORK_A.getId())
      .withPriority(10)
      .build();
    var r2 = FareLegRule.of(id("r2"), List.of(FARE_PRODUCT_B))
      .withNetworkId(NETWORK_A.getId())
      .withPriority(10)
      .build();
    var service = new FareLookupService(List.of(r1, r2), List.of(), EMPTY_STOP_AREAS);
    assertThat(service.legRules(leg())).containsExactly(r1, r2);
  }

  private TransitLeg leg() {
    return newItinerary(Place.forStop(STOP_1))
      .bus(ROUTE, 100, 100, 160, Place.forStop(STOP_2))
      .build()
      .transitLeg(0);
  }
}
