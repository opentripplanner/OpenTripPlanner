package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FreeTransferTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  FeedScopedId LEG_GROUP1 = id("leg-group1");
  int ID = 100;

  FareProduct freeTransfer = FareProduct.of(
    id("free-transfer"),
    "Free transfer",
    Money.ZERO_USD
  ).build();

  Place INNER_ZONE_STOP = Place.forStop(
    TEST_MODEL.stop("inner city stop").withCoordinate(1, 1).build()
  );
  Place OUTER_ZONE_STOP = Place.forStop(
    TEST_MODEL.stop("outer city stop").withCoordinate(2, 2).build()
  );
  String INNER_ZONE = "inner-zone";
  String OUTER_ZONE = "outer-zone";

  FareProduct regular = FareProduct.of(
    id( "regular"),
    "regular",
    Money.euros(5)
  ).build();

  GtfsFaresV2Service service = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("6"), regular)
        .withLegGroupId(LEG_GROUP1)
        .withFromAreaId(INNER_ZONE)
        .withToAreaId(INNER_ZONE)
        .build()
    ),
    List.of(
      new FareTransferRule(id("transfer"), LEG_GROUP1, LEG_GROUP1, -1, null, List.of(freeTransfer))
    ),
    Multimaps.forMap(
      Map.of(INNER_ZONE_STOP.stop.getId(), INNER_ZONE, OUTER_ZONE_STOP.stop.getId(), OUTER_ZONE)
    )
  );

  @Test
  void freeTransfer() {
    var i1 = newItinerary(INNER_ZONE_STOP, 0).bus(ID, 0, 50, INNER_ZONE_STOP).build();
    var result = service.getProducts(i1);
    assertEquals(Set.of(regular), result.itineraryProducts());
  }
}
