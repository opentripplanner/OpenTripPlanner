package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;

class PerStopCandidateCapTest {

  private static final int T = 8 * 3600;
  private static final int MIN = 60;
  private static final int WINDOW = 120 * MIN;

  /** A candidate: transit stop, when the passenger leaves, when the passenger arrives. */
  private record C(String name, int stop, int dep, int arr) {}

  /** A cap of 6: the first and the last car, three window slots of 40 minutes, one overflow slot. */
  private static PerStopCandidateCap<C> cap(boolean access, boolean arriveBy) {
    return new PerStopCandidateCap<>(6, access, arriveBy, T, WINDOW);
  }

  private static void add(PerStopCandidateCap<C> cap, C c) {
    cap.add(c, c.stop(), c.dep(), c.arr());
  }

  private static Set<String> names(PerStopCandidateCap<C> cap) {
    return cap.kept().stream().map(C::name).collect(Collectors.toSet());
  }

  @Test
  void aStopWithinTheCapHandsOverEveryCandidate() {
    var cap = cap(false, false);
    for (int m = 0; m < 6; m++) {
      add(cap, new C("t+" + m, 1, T + m * MIN, T + (m + 20) * MIN));
    }
    add(cap, new C("other-stop", 2, T + 3 * MIN, T + 30 * MIN));

    // Six cars within six minutes share a slot, yet all of them are kept: the stop is under the cap.
    assertEquals(Set.of("t+0", "t+1", "t+2", "t+3", "t+4", "t+5", "other-stop"), names(cap));
    assertEquals(7, cap.added());
  }

  @Test
  void overTheCapAnEgressStopKeepsFirstLastAndTheLatestDeparturePerSlot() {
    var cap = cap(false, false);
    // Slots of 40 minutes: [0, 40), [40, 80), [80, 120), then overflow.
    add(cap, new C("slot0-early", 1, T + 2 * MIN, T + 20 * MIN));
    add(cap, new C("slot0-late", 1, T + 30 * MIN, T + 50 * MIN));
    add(cap, new C("slot1-early", 1, T + 45 * MIN, T + 60 * MIN));
    add(cap, new C("slot1-mid", 1, T + 60 * MIN, T + 75 * MIN));
    add(cap, new C("slot1-late", 1, T + 79 * MIN, T + 95 * MIN));
    add(cap, new C("slot2-early", 1, T + 85 * MIN, T + 100 * MIN));
    add(cap, new C("slot2-late", 1, T + 110 * MIN, T + 130 * MIN));
    add(cap, new C("after-window", 1, T + 150 * MIN, T + 170 * MIN));

    // The first car survives although it is not the latest of its slot.
    assertEquals(
      Set.of("slot0-early", "slot0-late", "slot1-late", "slot2-late", "after-window"),
      names(cap)
    );
  }

  @Test
  void overTheCapAnAccessStopKeepsTheEarliestArrivalPerSlot() {
    var cap = cap(true, false);
    add(cap, new C("slot0-slow", 1, T + 2 * MIN, T + 50 * MIN));
    add(cap, new C("slot0-fast", 1, T + 10 * MIN, T + 30 * MIN));
    add(cap, new C("slot0-last", 1, T + 35 * MIN, T + 70 * MIN));
    add(cap, new C("slot1-slow", 1, T + 45 * MIN, T + 90 * MIN));
    add(cap, new C("slot1-fast", 1, T + 60 * MIN, T + 80 * MIN));
    add(cap, new C("slot2-a", 1, T + 90 * MIN, T + 110 * MIN));
    add(cap, new C("slot2-b", 1, T + 100 * MIN, T + 125 * MIN));

    // slot0-slow is the first car, slot2-b the last; the slots keep the earliest arrivals.
    assertEquals(
      Set.of("slot0-slow", "slot0-fast", "slot1-fast", "slot2-a", "slot2-b"),
      names(cap)
    );
  }

  @Test
  void carsAfterTheWindowShareAnOverflowSlotKeepingTheFirstOfThem() {
    var cap = cap(false, false);
    for (int m = 0; m < 7; m++) {
      add(cap, new C("in-window-" + m, 1, T + m * 15 * MIN, T + (m * 15 + 20) * MIN));
    }
    add(cap, new C("just-after", 1, T + 125 * MIN, T + 140 * MIN));
    add(cap, new C("hours-later", 1, T + 400 * MIN, T + 420 * MIN));

    var kept = names(cap);
    assertTrue(kept.contains("just-after"), kept.toString());
    assertTrue(!kept.contains("hours-later"), kept.toString());
    assertTrue(kept.contains("in-window-0") && kept.contains("in-window-6"), kept.toString());
    assertTrue(kept.size() <= 6, kept.toString());
  }

  @Test
  void dropsWhatLeavesBeforeTheRequest() {
    var cap = cap(true, false);
    add(cap, new C("before-request", 1, T - 1, T + 30 * MIN));
    add(cap, new C("at-request", 1, T, T + 40 * MIN));

    assertEquals(Set.of("at-request"), names(cap));
    assertEquals(1, cap.unusable());
  }

  @Test
  void neverHandsOverMoreThanTheCapPerStop() {
    var cap = new PerStopCandidateCap<C>(
      CarpoolingParameters.DEFAULT.maxCandidatesPerStop(),
      false,
      false,
      T,
      WINDOW
    );
    for (int m = 0; m < 120; m += 2) {
      add(cap, new C("t+" + m, 1, T + m * MIN, T + (m + 30) * MIN));
    }
    var kept = names(cap);
    assertTrue(kept.size() <= CarpoolingParameters.DEFAULT.maxCandidatesPerStop(), kept.toString());
    assertTrue(kept.size() >= 20, "one car per slot is kept: " + kept);
    assertTrue(kept.contains("t+0") && kept.contains("t+118"), kept.toString());
  }

  @Test
  void arriveByMirrorsTheTimeAxis() {
    // The request pins the arrivals. The egress is now the anchored leg: candidates arriving after
    // the requested time are useless, slots run backwards from it, and per slot the latest
    // departure from the stop is kept (the best free end).
    var egress = cap(false, true);
    add(egress, new C("too-late", 1, T - 10 * MIN, T + 1));
    add(egress, new C("slot0-early-leave", 1, T - 35 * MIN, T - 10 * MIN));
    add(egress, new C("slot0-late-leave", 1, T - 25 * MIN, T - 5 * MIN));
    add(egress, new C("slot0-last", 1, T - 60 * MIN, T - 39 * MIN));
    add(egress, new C("slot1-early-leave", 1, T - 75 * MIN, T - 60 * MIN));
    add(egress, new C("slot1-late-leave", 1, T - 70 * MIN, T - 50 * MIN));
    add(egress, new C("slot2-a", 1, T - 110 * MIN, T - 90 * MIN));
    add(egress, new C("slot2-b", 1, T - 130 * MIN, T - 115 * MIN));
    // Seven usable cars exceed the cap of six. slot0-late-leave is also the first car (arrives
    // closest to the requested time) and slot2-b the last one within the window.
    assertEquals(
      Set.of("slot0-late-leave", "slot1-late-leave", "slot2-a", "slot2-b"),
      names(egress)
    );
    assertEquals(1, egress.unusable());
  }

  @Test
  void arguments() {
    assertThrows(IllegalArgumentException.class, () ->
      new PerStopCandidateCap<C>(3, true, false, T, WINDOW)
    );
    assertThrows(IllegalArgumentException.class, () ->
      new PerStopCandidateCap<C>(6, true, false, T, 0)
    );
    assertEquals(List.of(), cap(true, false).kept());
  }
}
