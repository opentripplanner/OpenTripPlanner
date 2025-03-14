package org.opentripplanner.model.plan;

import static java.time.Duration.ZERO;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;

public class ItineraryTest implements PlanTestConstants {

  private static final Cost COST = Cost.costOfSeconds(720);

  @Test
  void testDerivedFieldsWithWalkingOnly() {
    Itinerary result = newItinerary(A, T11_00).walk(D5m, B).build();

    // Expected fields on itinerary set
    assertEquals(ofMinutes(5), result.totalDuration());
    assertEquals(0, result.numberOfTransfers());
    assertEquals(600, result.generalizedCost());
    assertEquals(ZERO, result.totalTransitDuration());
    assertEquals(ofMinutes(5), result.totalStreetDuration());
    assertEquals(ZERO, result.totalWaitingDuration());
    assertTrue(result.isWalkOnly());

    // Expected fields on walking leg set
    assertSameLocation(A, result.firstLeg().getFrom());
    assertEquals(newTime(T11_00), result.firstLeg().getStartTime());
    assertEquals(newTime(T11_05), result.firstLeg().getEndTime());
    assertEquals(TraverseMode.WALK, result.streetLeg(0).getMode());
    assertEquals(420.0d, result.firstLeg().getDistanceMeters(), 1E-3);
    assertSameLocation(B, result.lastLeg().getTo());

    assertEquals("A ~ Walk 5m ~ B [C₁600]", result.toStr());
  }

  @Test
  void testDerivedFieldsWithBusAllTheWay() {
    Itinerary result = newItinerary(A).bus(55, T11_00, T11_10, B).build();

    assertEquals(ofMinutes(10), result.totalDuration());
    assertEquals(0, result.numberOfTransfers());
    assertEquals(COST.toSeconds(), result.generalizedCost());
    assertEquals(ofMinutes(10), result.totalTransitDuration());
    assertEquals(ZERO, result.totalStreetDuration());
    assertEquals(ZERO, result.totalWaitingDuration());
    assertFalse(result.isWalkOnly());

    // Expected fields on bus leg set
    assertSameLocation(A, result.firstLeg().getFrom());
    assertSameLocation(B, result.firstLeg().getTo());
    assertEquals(newTime(T11_00), result.firstLeg().getStartTime());
    assertEquals(newTime(T11_10), result.firstLeg().getEndTime());
    assertEquals(TransitMode.BUS, result.transitLeg(0).getMode());
    assertEquals(TimetableRepositoryForTest.id("55"), result.firstLeg().getTrip().getId());
    assertEquals(7500, result.firstLeg().getDistanceMeters(), 1E-3);

    assertEquals("A ~ BUS 55 11:00 11:10 ~ B [C₁720]", result.toStr());
  }

  @Test
  void testDerivedFieldsWithTrainAllTheWay() {
    Itinerary result = newItinerary(A).rail(20, T11_05, T11_15, B).build();

    assertEquals(ofMinutes(10), result.totalDuration());
    assertEquals(0, result.numberOfTransfers());
    assertEquals(COST.toSeconds(), result.generalizedCost());
    assertEquals(ofMinutes(10), result.totalTransitDuration());
    assertEquals(ZERO, result.totalStreetDuration());
    assertEquals(ZERO, result.totalWaitingDuration());
    assertFalse(result.isWalkOnly());

    // Expected fields on bus leg set
    assertSameLocation(A, result.firstLeg().getFrom());
    assertSameLocation(B, result.firstLeg().getTo());
    assertEquals(newTime(T11_05), result.firstLeg().getStartTime());
    assertEquals(newTime(T11_15), result.firstLeg().getEndTime());
    assertEquals(TransitMode.RAIL, result.transitLeg(0).getMode());
    assertEquals(TimetableRepositoryForTest.id("20"), result.firstLeg().getTrip().getId());
    assertEquals(15_000, result.firstLeg().getDistanceMeters(), 1E-3);

    assertEquals("A ~ RAIL R2 11:05 11:15 ~ B [C₁720]", result.toStr());
  }

  @Test
  void testDerivedFieldsWithWalAccessAndTwoTransitLegs() {
    Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T11_02)
      .walk(D1m, B)
      .bus(21, T11_05, T11_10, C)
      .rail(110, T11_15, T11_30, D)
      .build();

    assertEquals(1, itinerary.numberOfTransfers());
    assertEquals(ofMinutes(28), itinerary.totalDuration());
    assertEquals(ofMinutes(20), itinerary.totalTransitDuration());
    assertEquals(ofMinutes(1), itinerary.totalStreetDuration());
    assertEquals(ofMinutes((2 + 5)), itinerary.totalWaitingDuration());
    // Cost: walk + wait + board + transit = 2 * 60 + .8 * 420 + 2 * 120 + 1200
    assertEquals(1896, itinerary.generalizedCost());

    assertEquals(60 * 1.4, itinerary.totalStreetDistanceMeters(), 0.01);
    assertFalse(itinerary.isWalkOnly());
  }

  @Test
  void testDerivedFieldsWithBusAndWalkingAccessAndEgress() {
    Itinerary result = newItinerary(A, T11_05)
      .walk(D2m, B)
      // 3 minutes wait
      .bus(1, T11_10, T11_20, C)
      .walk(D3m, D)
      .build();

    assertEquals(ofMinutes(18), result.totalDuration());
    assertEquals(0, result.numberOfTransfers());
    assertEquals(ofMinutes(10), result.totalTransitDuration());
    assertEquals(ofMinutes(5), result.totalStreetDuration());
    assertEquals(ofMinutes(3), result.totalWaitingDuration());
    // Cost: walk + wait + board + transit = 2 * 300 + .8 * 180 + 120 + 600
    assertEquals(1464, result.generalizedCost());
    assertFalse(result.isWalkOnly());

    assertEquals("A ~ Walk 2m ~ B ~ BUS 1 11:10 11:20 ~ C ~ Walk 3m ~ D [C₁1_464]", result.toStr());
  }

  @Test
  void walkBusBusWalkTrainWalk() {
    Itinerary result = newItinerary(A, T11_00)
      .walk(D2m, B)
      .bus(55, T11_04, T11_14, C)
      .bus(21, T11_16, T11_20, D)
      .walk(D3m, E)
      .rail(20, T11_30, T11_50, F)
      .walk(D1m, G)
      .build();

    assertEquals(ofMinutes(51), result.totalDuration());
    assertEquals(2, result.numberOfTransfers());
    assertEquals(ofMinutes(34), result.totalTransitDuration());
    assertEquals(ofMinutes(6), result.totalStreetDuration());
    assertEquals(ofMinutes(11), result.totalWaitingDuration());
    assertFalse(result.isWalkOnly());
    assertSameLocation(A, result.firstLeg().getFrom());
    assertSameLocation(G, result.lastLeg().getTo());

    assertEquals(
      "A ~ Walk 2m ~ B ~ BUS 55 11:04 11:14 ~ C ~ BUS 21 11:16 11:20 ~ D " +
      "~ Walk 3m ~ E ~ RAIL R2 11:30 11:50 ~ F ~ Walk 1m ~ G [C₁3_648]",
      result.toStr()
    );
  }

  @Test
  void walkSeparateFromBike() {
    var itin = newItinerary(A, T11_00).walk(D2m, B).bicycle(T11_05, T11_15, D).walk(D3m, E).build();

    assertEquals(ofMinutes(15), itin.totalStreetDuration());
    assertEquals(ofMinutes(5), itin.totalWalkDuration());

    assertEquals(420, itin.totalWalkDistanceMeters());
    assertEquals(3420, itin.totalStreetDistanceMeters());
  }

  @Test
  void walkSeparateFromCar() {
    var itin = newItinerary(A, T11_00).walk(D2m, B).carHail(D10m, D).walk(D3m, E).build();

    assertEquals(ofMinutes(15), itin.totalStreetDuration());
    assertEquals(ofMinutes(5), itin.totalWalkDuration());

    assertEquals(420, itin.totalWalkDistanceMeters());
    assertEquals(15420.0, itin.totalStreetDistanceMeters());
  }

  @Test
  void legIndex() {
    var itinerary = newItinerary(A, T11_00)
      .walk(D2m, B)
      .bus(55, T11_04, T11_14, C)
      .bus(21, T11_16, T11_20, D)
      .walk(D3m, E)
      .rail(20, T11_30, T11_50, F)
      .walk(D1m, G)
      .build();

    var leg = itinerary.legs().get(0);
    var oneHourLater = leg.withTimeShift(Duration.ofHours(1));

    assertNotSame(leg, oneHourLater);

    assertEquals(0, itinerary.findLegIndex(leg));
    assertEquals(0, itinerary.findLegIndex(oneHourLater));
  }

  @Test
  void hasSystemTag() {
    var subject = newItinerary(A).bus(1, T11_04, T11_14, B).build();
    subject.flagForDeletion(new SystemNotice("MY-TAG", "Text"));
    assertTrue(subject.hasSystemNoticeTag("MY-TAG"));
  }

  @Nested
  class AccessEgressPenalty {

    private static final TimeAndCost PENALTY = new TimeAndCost(
      Duration.ofMinutes(10),
      Cost.costOfMinutes(2)
    );

    @Test
    void noPenalty() {
      var subject = itineraryBuilder().withGeneralizedCost(COST).build();
      assertEquals(COST.toSeconds(), subject.generalizedCost());
      assertEquals(COST, subject.generalizedCostIncludingPenalty());
    }

    @Test
    void accessPenalty() {
      var subject = itineraryBuilder().withGeneralizedCost(COST).withAccessPenalty(PENALTY).build();
      assertEquals(COST.minus(PENALTY.cost()).toSeconds(), subject.generalizedCost());
      assertEquals(COST, subject.generalizedCostIncludingPenalty());
    }

    @Test
    void egressPenalty() {
      var subject = itineraryBuilder().withGeneralizedCost(COST).withEgressPenalty(PENALTY).build();
      assertEquals(COST.minus(PENALTY.cost()).toSeconds(), subject.generalizedCost());
      assertEquals(COST, subject.generalizedCostIncludingPenalty());
    }

    @Test
    void bothPenalties() {
      var subject = itineraryBuilder()
        .withGeneralizedCost(COST)
        .withAccessPenalty(PENALTY)
        .withEgressPenalty(PENALTY)
        .build();
      assertEquals(COST.minus(PENALTY.cost().multiply(2)).toSeconds(), subject.generalizedCost());
      assertEquals(COST, subject.generalizedCostIncludingPenalty());
    }

    @Test
    void directFlex() {
      assertFalse(itinerary().isDirectFlex());
      assertTrue(newItinerary(A).flex(T11_10, T11_20, B).build().isDirectFlex());
    }

    @Test
    void walkOnlyIsNotDirectFlex() {
      assertFalse(TestItineraryBuilder.newItinerary(A, T11_00).walk(10, B).build().isDirectFlex());
    }

    private Itinerary itinerary() {
      return itineraryBuilder().build();
    }

    private ItineraryBuilder itineraryBuilder() {
      return newItinerary(A).bus(1, T11_04, T11_14, B).itineraryBuilder();
    }
  }

  private void assertSameLocation(Place expected, Place actual) {
    assertTrue(
      expected.sameLocation(actual),
      "Same location? Expected: " + expected + ", actual: " + actual
    );
  }
}
