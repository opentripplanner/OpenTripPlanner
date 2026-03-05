package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.transit.model.basic.TransitMode;

class TransitPreferencesTest {

  private static final int OTHER_THAN_PREFERRED_ROUTES_PENALTY = 350;
  private static final Map<TransitMode, Double> RELUCTANCE_FOR_MODE = Map.of(
    TransitMode.AIRPLANE,
    2.1
  );
  private static final CostLinearFunction UNPREFERRED_COST = CostLinearFunction.of("5m + 1.15 x");
  private static final Duration D15_s = Duration.ofSeconds(15);
  private static final Duration D45_s = Duration.ofSeconds(45);
  private static final Duration D25_m = Duration.ofMinutes(25);
  private static final Duration D35_m = Duration.ofMinutes(35);
  private static final SearchDirection RAPTOR_SEARCH_DIRECTION = SearchDirection.REVERSE;
  private static final CostLinearFunction TRANSIT_GROUP_PRIORITY_RELAX = CostLinearFunction.of(
    Cost.costOfSeconds(300),
    1.5
  );
  private static final boolean IGNORE_REALTIME_UPDATES = true;
  private static final boolean INCLUDE_PLANNED_CANCELLATIONS = true;
  private static final boolean INCLUDE_REALTIME_CANCELLATIONS = true;

  private final TransitPreferences subject = TransitPreferences.of()
    .withReluctanceForMode(RELUCTANCE_FOR_MODE)
    .withOtherThanPreferredRoutesPenalty(OTHER_THAN_PREFERRED_ROUTES_PENALTY)
    .withUnpreferredCost(UNPREFERRED_COST)
    .withBoardSlack(b -> b.withDefault(D45_s).with(TransitMode.AIRPLANE, D35_m))
    .withAlightSlack(b -> b.withDefault(D15_s).with(TransitMode.AIRPLANE, D25_m))
    .withRelaxTransitGroupPriority(TRANSIT_GROUP_PRIORITY_RELAX)
    .withIgnoreRealtimeUpdates(IGNORE_REALTIME_UPDATES)
    .withIncludePlannedCancellations(INCLUDE_PLANNED_CANCELLATIONS)
    .withIncludeRealtimeCancellations(INCLUDE_REALTIME_CANCELLATIONS)
    .withRaptor(b -> b.withSearchDirection(RAPTOR_SEARCH_DIRECTION))
    .build();

  @Test
  void boardSlack() {
    assertEquals(D45_s, subject.boardSlack().defaultValue());
    assertEquals(D35_m, subject.boardSlack().valueOf(TransitMode.AIRPLANE));
  }

  @Test
  void alightSlack() {
    assertEquals(D15_s, subject.alightSlack().defaultValue());
    assertEquals(D25_m, subject.alightSlack().valueOf(TransitMode.AIRPLANE));
  }

  @Test
  void reluctanceForMode() {
    assertEquals(RELUCTANCE_FOR_MODE, subject.reluctanceForMode());
  }

  @Test
  void otherThanPreferredRoutesPenalty() {
    assertEquals(OTHER_THAN_PREFERRED_ROUTES_PENALTY, subject.otherThanPreferredRoutesPenalty());
  }

  @Test
  void unpreferredCost() {
    assertEquals(UNPREFERRED_COST, subject.unpreferredCost());
  }

  @Test
  void relaxTransitGroupPriority() {
    assertEquals(TRANSIT_GROUP_PRIORITY_RELAX, subject.relaxTransitGroupPriority());
  }

  @Test
  void isRelaxTransitGroupPrioritySet() {
    assertTrue(subject.isRelaxTransitGroupPrioritySet());
    assertFalse(TransitPreferences.DEFAULT.isRelaxTransitGroupPrioritySet());
  }

  @Test
  void ignoreRealtimeUpdates() {
    assertFalse(TransitPreferences.DEFAULT.ignoreRealtimeUpdates());
    assertTrue(subject.ignoreRealtimeUpdates());
  }

  @Test
  void includePlannedCancellations() {
    assertFalse(TransitPreferences.DEFAULT.includePlannedCancellations());
    assertTrue(subject.includePlannedCancellations());
  }

  @Test
  void includeRealtimeCancellations() {
    assertFalse(TransitPreferences.DEFAULT.includeRealtimeCancellations());
    assertTrue(subject.includeRealtimeCancellations());
  }

  @Test
  void raptorOptions() {
    assertEquals(RAPTOR_SEARCH_DIRECTION, subject.raptor().searchDirection());
  }

  @Test
  void testEquals() {
    // Return same object if no value is set
    assertSame(subject, subject.copyOf().build());
    assertSame(TransitPreferences.DEFAULT, TransitPreferences.of().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withIgnoreRealtimeUpdates(!IGNORE_REALTIME_UPDATES).build();
    var copy = other.copyOf().withIgnoreRealtimeUpdates(IGNORE_REALTIME_UPDATES).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals(
      "TransitPreferences{" +
        "boardSlack: DurationForTransitMode{default:45s, AIRPLANE:35m}, " +
        "alightSlack: DurationForTransitMode{default:15s, AIRPLANE:25m}, " +
        "reluctanceForMode: {AIRPLANE=2.1}, " +
        "otherThanPreferredRoutesPenalty: $350, " +
        "unpreferredCost: 5m + 1.15 t, " +
        "relaxTransitGroupPriority: 5m + 1.50 t, " +
        "ignoreRealtimeUpdates, " +
        "includePlannedCancellations, " +
        "includeRealtimeCancellations, " +
        "raptor: RaptorPreferences{searchDirection: REVERSE}" +
        "}",
      subject.toString()
    );
  }
}
