package org.opentripplanner.ext.fares.impl.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class AreasTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest MODEL = TimetableRepositoryForTest.of();

  private static final FeedScopedId LEG_GROUP1 = id("leg-group1");
  private static final int ID = 100;

  private static final FareProduct SINGLE_TO_OUTER = FareProduct.of(
    new FeedScopedId(FEED_ID, "single_to_outer"),
    "Single one-way ticket to outer zone",
    Money.euros(1)
  ).build();
  private static final FareProduct SINGLE_FROM_OUTER = FareProduct.of(
    new FeedScopedId(FEED_ID, "single_from_outer"),
    "Single one-way ticket from outer zone to anywhere",
    Money.euros(1)
  ).build();
  private static final FareProduct INNER_TO_OUTER_ZONE_SINGLE = FareProduct.of(
    new FeedScopedId(FEED_ID, "zone_ab_single"),
    "Day Pass",
    Money.euros(5)
  ).build();

  private static final Place INNER_ZONE_PLACE = Place.forStop(
    MODEL.stop("inner city stop").withCoordinate(1, 1).build()
  );
  private static final Place OUTER_ZONE_PLACE = Place.forStop(
    MODEL.stop("outer city stop").withCoordinate(2, 2).build()
  );
  private static final FeedScopedId INNER_ZONE = id("inner-zone");
  private static final FeedScopedId OUTER_ZONE = id("outer-zone");

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("2"), SINGLE_TO_OUTER)
        .withLegGroupId(LEG_GROUP1)
        .withToAreaId(OUTER_ZONE)
        .build(),
      FareLegRule.of(id("3"), SINGLE_FROM_OUTER)
        .withLegGroupId(LEG_GROUP1)
        .withFromAreaId(OUTER_ZONE)
        .build(),
      FareLegRule.of(id("6"), INNER_TO_OUTER_ZONE_SINGLE)
        .withLegGroupId(LEG_GROUP1)
        .withFromAreaId(INNER_ZONE)
        .withToAreaId(OUTER_ZONE)
        .build()
    ),
    List.of(),
    Multimaps.forMap(
      Map.of(INNER_ZONE_PLACE.stop.getId(), INNER_ZONE, OUTER_ZONE_PLACE.stop.getId(), OUTER_ZONE)
    )
  );

  @Test
  void twoAreaIds() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, INNER_ZONE_PLACE)
      .faresV2Rail(ID, 0, 50, OUTER_ZONE_PLACE, null)
      .build();

    var result = SERVICE.calculateFares(i1);
    assertEquals(
      Set.of(
        FareOffer.of(
          i1.listScheduledTransitLegs().getFirst().startTime(),
          INNER_TO_OUTER_ZONE_SINGLE
        )
      ),
      result.offersForLeg(i1.legs().get(1))
    );
  }

  @Test
  void onlyToAreaId() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, B)
      .faresV2Rail(ID, 0, 50, OUTER_ZONE_PLACE, null)
      .build();

    var result = SERVICE.calculateFares(i1);
    assertEquals(
      Set.of(FareOffer.of(i1.listScheduledTransitLegs().getFirst().startTime(), SINGLE_TO_OUTER)),
      result.offersForLeg(i1.legs().get(1))
    );
  }

  @Test
  void onlyFromAreaId() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, OUTER_ZONE_PLACE)
      .faresV2Rail(ID, 0, 50, B, null)
      .build();

    var result = SERVICE.calculateFares(i1);
    assertEquals(
      Set.of(FareOffer.of(i1.listScheduledTransitLegs().getFirst().startTime(), SINGLE_FROM_OUTER)),
      result.offersForLeg(i1.legs().get(1))
    );
  }
}
