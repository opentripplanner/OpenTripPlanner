package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.ImmutableMultimap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.site.RegularStop;

class FareLookupServiceTest implements FareTestConstants {

  private static final FeedScopedId A_1 = id("a1");
  public static final FeedScopedId N_1 = TimetableRepositoryForTest.id("n1");
  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  private final RegularStop STOP_1 = testModel.stop("stop1").build();
  private final RegularStop STOP_2 = testModel.stop("stop2").build();

  @Test
  void test() {
    var r1 = FareLegRule.of(id("r1"), List.of(FARE_PRODUCT_A)).withNetworkId(N_1).build();
    var r2 = FareLegRule.of(id("r2"), List.of(FARE_PRODUCT_B))
      .withFromAreaId(A_1)
      .withToAreaId(A_1)
      .build();
    var service = new FareLookupService(
      List.of(r1, r2),
      List.of(),
      ImmutableMultimap.of(STOP_1.getId(), A_1, STOP_2.getId(), A_1)
    );

    var route = TimetableRepositoryForTest.route("r1")
      .withGroupOfRoutes(List.of(GroupOfRoutes.of(N_1).build()))
      .build();

    var leg = newItinerary(Place.forStop(STOP_1))
      .bus(route, 100, 100, 160, Place.forStop(STOP_2))
      .build()
      .transitLeg(0);
    var rules = service.legRules(leg);
    assertThat(rules).isEmpty();
  }
}
