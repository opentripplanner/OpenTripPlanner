package org.opentripplanner.ext.fares.service.gtfs.v2;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import com.google.common.collect.ImmutableMultimap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTestConstants;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;

class FlexLegTest implements PlanTestConstants, FareTestConstants {

  private static final FeedScopedId LEG_GROUP = id("leg-group-a");
  private static final FeedScopedId STOP_AREA = id("stop-area-a");

  private static final GtfsFaresV2Service SERVICE = GtfsFaresV2Service.of()
    .withLegRules(
      FareLegRule.of(id("r1"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP)
        .withFromAreaId(STOP_AREA)
        .withToAreaId(STOP_AREA)
        .build()
    )
    .withTransferRules(List.of())
    .withStopAreas(ImmutableMultimap.of(A.stop.getId(), STOP_AREA))
    .build();

  @Test
  void flexLeg() {
    var i1 = newItinerary(A, 0).flex(T11_00, T11_05, A).build();
    var result = SERVICE.calculateFares(i1);

    assertThat(result.itineraryProducts()).isEmpty();
    var first = i1.legs().getFirst();
    assertThat(result.offersForLeg(first)).containsExactly(
      FareOffer.of(first.startTime(), FARE_PRODUCT_A)
    );
  }
}
