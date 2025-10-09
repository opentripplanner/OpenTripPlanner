package org.opentripplanner.ext.fares.impl.gtfs;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.ImmutableMultimap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.impl._support.FareTestConstants;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FlexLegTest implements PlanTestConstants, FareTestConstants {

  private static final FeedScopedId LEG_GROUP = id("leg-group-a");
  private static final FeedScopedId STOP_AREA = id("stop-area-a");

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("r1"), FARE_PRODUCT_A)
        .withLegGroupId(LEG_GROUP)
        .withFromAreaId(STOP_AREA)
        .withToAreaId(STOP_AREA)
        .build()
    ),
    List.of(),
    ImmutableMultimap.of(A.stop.getId(), STOP_AREA)
  );

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
