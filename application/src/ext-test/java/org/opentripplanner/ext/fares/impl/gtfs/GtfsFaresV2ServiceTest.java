package org.opentripplanner.ext.fares.impl.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import com.google.common.collect.ImmutableMultimap;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GtfsFaresV2ServiceTest implements PlanTestConstants {

  private static final FeedScopedId LEG_GROUP1 = id("leg-group1");
  private static final int ID = 100;
  private static final FeedScopedId expressNetwork = id("express");
  private static final FeedScopedId localNetwork = id("local");

  private static final FareProduct ALL_NETWORKS_SINGLE = FareProduct.of(
    new FeedScopedId(FEED_ID, "single"),
    "Single one-way ticket",
    Money.euros(1)
  ).build();
  private static final FareProduct ALL_NETWORKS_DAY_PASS = FareProduct.of(
    new FeedScopedId(FEED_ID, "day_pass"),
    "Day Pass",
    Money.euros(5)
  )
    .withValidity(Duration.ofDays(1))
    .build();
  private static final FareProduct MONTHLY_PASS = FareProduct.of(
    new FeedScopedId("another", "monthly_pass"),
    "Monthly Pass",
    Money.euros(30)
  )
    .withValidity(Duration.ofDays(30))
    .build();
  private static final FareProduct EXPRESS_DAY_PASS = FareProduct.of(
    new FeedScopedId(FEED_ID, "express_pass"),
    "Express Pass",
    Money.euros(50)
  )
    .withValidity(Duration.ofDays(1))
    .build();
  private static final FareProduct LOCAL_DAY_PASS = FareProduct.of(
    new FeedScopedId(FEED_ID, "local_pass"),
    "Local Pass",
    Money.euros(20)
  )
    .withValidity(Duration.ofDays(1))
    .build();

  private static final GtfsFaresV2Service SERVICE = new GtfsFaresV2Service(
    List.of(
      FareLegRule.of(id("1"), ALL_NETWORKS_SINGLE).withLegGroupId(LEG_GROUP1).build(),
      FareLegRule.of(id("4"), ALL_NETWORKS_DAY_PASS).withLegGroupId(LEG_GROUP1).build(),
      FareLegRule.of(id("5"), EXPRESS_DAY_PASS)
        .withLegGroupId(LEG_GROUP1)
        .withNetworkId(expressNetwork)
        .build(),
      FareLegRule.of(id("5"), LOCAL_DAY_PASS)
        .withLegGroupId(LEG_GROUP1)
        .withNetworkId(localNetwork)
        .build(),
      FareLegRule.of(MONTHLY_PASS.id(), MONTHLY_PASS)
        .withLegGroupId(id("another-leg-group"))
        .build()
    ),
    List.of(),
    ImmutableMultimap.of()
  );

  @Test
  void singleLeg() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).build();

    var result = SERVICE.calculateFares(i1);
    var startTime = i1.listScheduledTransitLegs().getFirst().startTime();
    assertEquals(
      Set.of(
        FareOffer.of(startTime, ALL_NETWORKS_DAY_PASS),
        FareOffer.of(startTime, ALL_NETWORKS_SINGLE)
      ),
      result.offersForLeg(i1.legs().get(1))
    );
  }

  @Test
  void twoLegs() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).bus(ID, 0, 50, C).bus(ID, 55, 70, D).build();

    var result = SERVICE.calculateFares(i1);
    var startTime = i1.listScheduledTransitLegs().getFirst().startTime();
    assertEquals(
      Set.of(
        FareOffer.of(startTime, ALL_NETWORKS_DAY_PASS),
        FareOffer.of(startTime, ALL_NETWORKS_SINGLE)
      ),
      result.offersForLeg(i1.legs().get(1))
    );
  }

  @Test
  void networkId() {
    Itinerary i1 = newItinerary(A, 0).walk(20, B).faresV2Rail(ID, 0, 50, C, expressNetwork).build();

    var result = SERVICE.calculateFares(i1);
    var startTime = i1.listScheduledTransitLegs().getFirst().startTime();
    assertEquals(
      Set.of(FareOffer.of(startTime, EXPRESS_DAY_PASS)),
      result.offersForLeg(i1.legs().get(1))
    );
  }

  /**
   * Because we use both the local and the express network, there is no product covering both.
   */
  @Test
  void separateFares() {
    Itinerary i1 = newItinerary(A, 0)
      .walk(20, A)
      .faresV2Rail(ID, 0, 50, B, localNetwork)
      .faresV2Rail(ID, 60, 100, C, expressNetwork)
      .build();

    var result = SERVICE.calculateFares(i1);
    assertEquals(0, result.itineraryProducts().size());

    var localLeg = i1.legs().get(1);
    var localLegProducts = result.offersForLeg(localLeg);
    assertEquals(Set.of(FareOffer.of(localLeg.startTime(), LOCAL_DAY_PASS)), localLegProducts);

    var expressLeg = i1.legs().get(2);
    var expressProducts = result.offersForLeg(expressLeg);
    assertEquals(Set.of(FareOffer.of(expressLeg.startTime(), EXPRESS_DAY_PASS)), expressProducts);
  }
}
