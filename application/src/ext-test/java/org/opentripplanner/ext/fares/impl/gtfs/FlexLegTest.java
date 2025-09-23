package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;

import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

class FlexLegTest implements PlanTestConstants, FareTestConstants {

  private static final Route ROUTE = route("r1").withGroupOfRoutes(List.of(NETWORK_A)).build();
  private static final FeedScopedId LEG_GROUP = id("leg-group-a");

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("r1"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP)
        .withNetworkId(NETWORK_A.getId())
        .build()
    ),
    List.of(),
    Multimaps.forMap(Map.of())
  );

  @Test
  void flexLeg() {
    var i1 = newItinerary(A, 0).flex(ROUTE, T11_00, T11_05, B).build();
    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();
    var first = i1.legs().getFirst();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
  }
}
