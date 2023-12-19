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
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;

public class ItineraryTest implements PlanTestConstants {

  @Test
  public void testDerivedFieldsWithWalkingOnly() {
    Itinerary result = newItinerary(A, T11_00).walk(D5m, B).build();

    // Expected fields on itinerary set
    assertEquals(ofMinutes(5), result.getDuration());
    assertEquals(0, result.getNumberOfTransfers());
    assertEquals(600, result.getGeneralizedCost());
    assertEquals(ZERO, result.getTransitDuration());
    assertEquals(ofMinutes(5), result.getNonTransitDuration());
    assertEquals(ZERO, result.getWaitingDuration());
    assertTrue(result.isWalkOnly());

    // Expected fields on walking leg set
    assertSameLocation(A, result.firstLeg().getFrom());
    assertEquals(newTime(T11_00), result.firstLeg().getStartTime());
    assertEquals(newTime(T11_05), result.firstLeg().getEndTime());
    assertEquals(TraverseMode.WALK, result.getStreetLeg(0).getMode());
    assertEquals(420.0d, result.firstLeg().getDistanceMeters(), 1E-3);
    assertSameLocation(B, result.lastLeg().getTo());

    assertEquals("A ~ Walk 5m ~ B [C₁600]", result.toStr());
  }

  @Test
  public void testDerivedFieldsWithBusAllTheWay() {
    Itinerary result = newItinerary(A).bus(55, T11_00, T11_10, B).build();

    assertEquals(ofMinutes(10), result.getDuration());
    assertEquals(0, result.getNumberOfTransfers());
    assertEquals(720, result.getGeneralizedCost());
    assertEquals(ofMinutes(10), result.getTransitDuration());
    assertEquals(ZERO, result.getNonTransitDuration());
    assertEquals(ZERO, result.getWaitingDuration());
    assertFalse(result.isWalkOnly());

    // Expected fields on bus leg set
    assertSameLocation(A, result.firstLeg().getFrom());
    assertSameLocation(B, result.firstLeg().getTo());
    assertEquals(newTime(T11_00), result.firstLeg().getStartTime());
    assertEquals(newTime(T11_10), result.firstLeg().getEndTime());
    assertEquals(TransitMode.BUS, result.getTransitLeg(0).getMode());
    assertEquals(TransitModelForTest.id("55"), result.firstLeg().getTrip().getId());
    assertEquals(7500, result.firstLeg().getDistanceMeters(), 1E-3);

    assertEquals("A ~ BUS 55 11:00 11:10 ~ B [C₁720]", result.toStr());
  }

  @Test
  public void testDerivedFieldsWithTrainAllTheWay() {
    Itinerary result = newItinerary(A).rail(20, T11_05, T11_15, B).build();

    assertEquals(ofMinutes(10), result.getDuration());
    assertEquals(0, result.getNumberOfTransfers());
    assertEquals(720, result.getGeneralizedCost());
    assertEquals(ofMinutes(10), result.getTransitDuration());
    assertEquals(ZERO, result.getNonTransitDuration());
    assertEquals(ZERO, result.getWaitingDuration());
    assertFalse(result.isWalkOnly());

    // Expected fields on bus leg set
    assertSameLocation(A, result.firstLeg().getFrom());
    assertSameLocation(B, result.firstLeg().getTo());
    assertEquals(newTime(T11_05), result.firstLeg().getStartTime());
    assertEquals(newTime(T11_15), result.firstLeg().getEndTime());
    assertEquals(TransitMode.RAIL, result.getTransitLeg(0).getMode());
    assertEquals(TransitModelForTest.id("20"), result.firstLeg().getTrip().getId());
    assertEquals(15_000, result.firstLeg().getDistanceMeters(), 1E-3);

    assertEquals("A ~ RAIL R2 11:05 11:15 ~ B [C₁720]", result.toStr());
  }

  @Test
  public void testDerivedFieldsWithWalAccessAndTwoTransitLegs() {
    Itinerary itinerary = TestItineraryBuilder
      .newItinerary(A, T11_02)
      .walk(D1m, B)
      .bus(21, T11_05, T11_10, C)
      .rail(110, T11_15, T11_30, D)
      .build();

    assertEquals(1, itinerary.getNumberOfTransfers());
    assertEquals(ofMinutes(28), itinerary.getDuration());
    assertEquals(ofMinutes(20), itinerary.getTransitDuration());
    assertEquals(ofMinutes(1), itinerary.getNonTransitDuration());
    assertEquals(ofMinutes((2 + 5)), itinerary.getWaitingDuration());
    // Cost: walk + wait + board + transit = 2 * 60 + .8 * 420 + 2 * 120 + 1200
    assertEquals(1896, itinerary.getGeneralizedCost());

    assertEquals(60 * 1.4, itinerary.getNonTransitDistanceMeters(), 0.01);
    assertFalse(itinerary.isWalkOnly());
  }

  @Test
  public void testDerivedFieldsWithBusAndWalkingAccessAndEgress() {
    Itinerary result = newItinerary(A, T11_05)
      .walk(D2m, B)
      // 3 minutes wait
      .bus(1, T11_10, T11_20, C)
      .walk(D3m, D)
      .build();

    assertEquals(ofMinutes(18), result.getDuration());
    assertEquals(0, result.getNumberOfTransfers());
    assertEquals(ofMinutes(10), result.getTransitDuration());
    assertEquals(ofMinutes(5), result.getNonTransitDuration());
    assertEquals(ofMinutes(3), result.getWaitingDuration());
    // Cost: walk + wait + board + transit = 2 * 300 + .8 * 180 + 120 + 600
    assertEquals(1464, result.getGeneralizedCost());
    assertFalse(result.isWalkOnly());

    assertEquals("A ~ Walk 2m ~ B ~ BUS 1 11:10 11:20 ~ C ~ Walk 3m ~ D [C₁1_464]", result.toStr());
  }

  @Test
  public void walkBusBusWalkTrainWalk() {
    Itinerary result = newItinerary(A, T11_00)
      .walk(D2m, B)
      .bus(55, T11_04, T11_14, C)
      .bus(21, T11_16, T11_20, D)
      .walk(D3m, E)
      .rail(20, T11_30, T11_50, F)
      .walk(D1m, G)
      .build();

    assertEquals(ofMinutes(51), result.getDuration());
    assertEquals(2, result.getNumberOfTransfers());
    assertEquals(ofMinutes(34), result.getTransitDuration());
    assertEquals(ofMinutes(6), result.getNonTransitDuration());
    assertEquals(ofMinutes(11), result.getWaitingDuration());
    assertEquals(720 + 528 + 360 + 2040, result.getGeneralizedCost());
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
  public void legIndex() {
    var itinerary = newItinerary(A, T11_00)
      .walk(D2m, B)
      .bus(55, T11_04, T11_14, C)
      .bus(21, T11_16, T11_20, D)
      .walk(D3m, E)
      .rail(20, T11_30, T11_50, F)
      .walk(D1m, G)
      .build();

    var leg = itinerary.getLegs().get(0);
    var oneHourLater = leg.withTimeShift(Duration.ofHours(1));

    assertNotSame(leg, oneHourLater);

    assertEquals(0, itinerary.getLegIndex(leg));
    assertEquals(0, itinerary.getLegIndex(oneHourLater));
  }

  @Test
  public void hasSystemTag() {
    var subject = newItinerary(A).bus(1, T11_04, T11_14, B).build();
    subject.flagForDeletion(new SystemNotice("MY-TAG", "Text"));
    assertTrue(subject.hasSystemNoticeTag("MY-TAG"));
  }

  private void assertSameLocation(Place expected, Place actual) {
    assertTrue(
      expected.sameLocation(actual),
      "Same location? Expected: " + expected + ", actual: " + actual
    );
  }
}
