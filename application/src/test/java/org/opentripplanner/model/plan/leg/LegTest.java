package org.opentripplanner.model.plan.leg;

import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.SERVICE_DAY;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.LocalDate;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;

public class LegTest implements PlanTestConstants {

  private static final int START_TIME = T11_00;

  private final Itinerary ITINERARY = newItinerary(A, START_TIME)
    .walk(D2m, B)
    .bus(21, T11_05, T11_15, C)
    .bicycle(T11_16, T11_20, E)
    .build();

  private final Leg WALK_LEG = ITINERARY.legs().getFirst();
  private final Leg BUS_LEG = ITINERARY.legs().get(1);
  private final Leg BICYCLE_LEG = ITINERARY.legs().getLast();

  @Test
  public void isTransitLeg() {
    assertFalse(WALK_LEG.isTransitLeg());
    assertTrue(BUS_LEG.isTransitLeg());
    assertFalse(BICYCLE_LEG.isTransitLeg());
  }

  @Test
  public void isWalkingLeg() {
    assertTrue(WALK_LEG.isWalkingLeg());
    assertFalse(BUS_LEG.isWalkingLeg());
    assertFalse(BICYCLE_LEG.isWalkingLeg());
  }

  @Test
  public void isOnStreetNonTransit() {
    assertTrue(WALK_LEG.isStreetLeg());
    assertFalse(BUS_LEG.isStreetLeg());
    assertTrue(BICYCLE_LEG.isStreetLeg());
  }

  @Test
  public void duration() {
    assertEquals(ofMinutes(2), WALK_LEG.duration());
    assertEquals(ofMinutes(10), BUS_LEG.duration());
    assertEquals(ofMinutes(4), BICYCLE_LEG.duration());
  }

  @Test
  public void isPartiallySameTransitLeg() {
    final int fromStopIndex = 2;
    final int toStopIndex = 12;
    final int tripId0 = 33;
    final int tripIdOther = 44;
    final LocalDate day1 = LocalDate.of(2020, 9, 17);
    final LocalDate day2 = LocalDate.of(2020, 9, 18);

    Leg t0 = createLegIgnoreTime(tripId0, fromStopIndex, toStopIndex, day1);
    Leg t1;

    assertTrue(t0.isPartiallySameTransitLeg(t0), "t0 is same trip as it self");

    // Create a new trip with the same trip Id which ride only between the two first stops of trip 0.
    t1 = createLegIgnoreTime(tripId0, fromStopIndex, fromStopIndex + 1, day1);
    assertTrue(t1.isPartiallySameTransitLeg(t0), "t1 overlap t0");
    assertTrue(t0.isPartiallySameTransitLeg(t1), "t0 overlap t1");

    // Create a new trip with the same trip Id but on a different service day
    t1 = createLegIgnoreTime(tripId0, fromStopIndex, fromStopIndex + 1, day2);
    assertFalse(t1.isPartiallySameTransitLeg(t0), "t1 different serviceDate from t0");
    assertFalse(t0.isPartiallySameTransitLeg(t1), "t0 different serviceDate from t1");

    // Create a new trip with the same trip Id which ride only between the two last stops of trip 0.
    t1 = createLegIgnoreTime(tripId0, toStopIndex - 1, toStopIndex, day1);
    assertTrue(t1.isPartiallySameTransitLeg(t0), "t1 overlap t0");
    assertTrue(t0.isPartiallySameTransitLeg(t1), "t0 overlap t1");

    // Create a new trip which alight at the board stop of t0 - should not overlap
    t1 = createLegIgnoreTime(tripId0, fromStopIndex - 1, fromStopIndex, day1);
    assertFalse(t1.isPartiallySameTransitLeg(t0), "t1 do not overlap t0");
    assertFalse(t0.isPartiallySameTransitLeg(t1), "t0 do not overlap t1");

    // Two legs do NOT overlap is on different trips
    t1 = createLegIgnoreTime(tripIdOther, fromStopIndex, toStopIndex, day1);
    assertFalse(t1.isPartiallySameTransitLeg(t0), "t1 do not overlap t0");
    assertFalse(t0.isPartiallySameTransitLeg(t1), "t0 do not overlap t1");
  }

  @Test
  public void isPartiallySameLeg() {
    final int startTime = START_TIME;
    final int duration = D10m;
    final int endTime = startTime + duration;
    final int fromStopIndex = 2;
    final int toStopIndex = 12;
    final int tripId = 33;
    final LocalDate day = SERVICE_DAY;

    Leg busLeg = leg(b -> b.bus(tripId, startTime, endTime, fromStopIndex, toStopIndex, B, day));
    Leg walkLeg = leg(b -> b.walk(duration, B));
    Leg bicycleLeg = leg(b -> b.bicycle(startTime, endTime, B));

    // same leg equals itself
    {
      assertTrue(busLeg.isPartiallySameLeg(busLeg));
      assertTrue(walkLeg.isPartiallySameLeg(walkLeg));
    }

    // Same values
    {
      Leg busLegSame = leg(b ->
        b.bus(tripId, startTime, endTime, fromStopIndex, toStopIndex, B, day)
      );
      assertTrue(busLeg.isPartiallySameLeg(busLegSame));

      Leg walkLegSame = leg(b -> b.walk(duration, B));
      assertTrue(walkLeg.isPartiallySameLeg(walkLegSame));
    }

    // Mode differ
    {
      assertFalse(busLeg.isPartiallySameLeg(walkLeg));
      assertFalse(bicycleLeg.isPartiallySameLeg(walkLeg));
    }

    // Time do not overlap
    {
      Leg walkLegAfter = leg(endTime, b -> b.walk(D12m, B));
      assertFalse(walkLeg.isPartiallySameLeg(walkLegAfter));
      assertFalse(walkLegAfter.isPartiallySameLeg(walkLeg));
    }

    // Trip id do not overlap
    {
      int tripIdOther = tripId + 11;
      Leg busLegOther = leg(b ->
        b.bus(tripIdOther, startTime, endTime, fromStopIndex, toStopIndex, B, day)
      );
      assertFalse(busLeg.isPartiallySameLeg(busLegOther));
    }

    // Same trip id, but different service day AND stop positions
    {
      var dayBefore = day.minusDays(1);
      Leg busOther = leg(b -> b.bus(tripId, startTime, endTime, 1, fromStopIndex, B, dayBefore));
      assertFalse(busLeg.isPartiallySameLeg(busOther));
    }
  }

  @Test
  public void overlapInTime() {
    int duration = D10m;
    var endTime = START_TIME + duration;
    var overlappingStartTime = START_TIME + duration - 1;

    Leg subject = leg(b -> b.bus(11, START_TIME, endTime, B));
    Leg overlappingLeg = leg(overlappingStartTime, b ->
      b.walk(duration, B).build().legs().getFirst()
    );
    Leg legAfter = leg(endTime, b -> b.walk(D12m, B));

    // Overlap in time
    assertTrue(overlappingLeg.overlapInTime(subject));
    assertTrue(subject.overlapInTime(overlappingLeg));

    // Do not overlap
    assertFalse(legAfter.overlapInTime(subject));
    assertFalse(subject.overlapInTime(legAfter));
  }

  private static Leg createLegIgnoreTime(
    int tripId,
    int fromStopIndex,
    int toStopIndex,
    LocalDate serviceDate
  ) {
    return leg(b ->
      b.bus(tripId, START_TIME, START_TIME + 99, fromStopIndex, toStopIndex, B, serviceDate)
    );
  }

  private static Leg leg(Consumer<TestItineraryBuilder> buildOp) {
    return leg(START_TIME, buildOp);
  }

  private static Leg leg(int startTime, Consumer<TestItineraryBuilder> buildOp) {
    var builder = newItinerary(A, startTime);
    buildOp.accept(builder);
    return builder.build().legs().getFirst();
  }
}
