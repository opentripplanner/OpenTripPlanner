package org.opentripplanner.updater.trip.gtfs.model;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model._data.PatternTestModel;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;

class StopPatternChangesTest {

  /**
   * A two-stop pattern whose stops all have {@code SCHEDULED} pickup and drop-off.
   */
  private final TripPattern plannedPattern = PatternTestModel.pattern();

  /**
   * A resolver that resolves no stop id, used when the update carries no resolvable stop
   * replacement.
   */
  private static final Function<String, StopLocation> RESOLVES_NOTHING = id -> null;

  @Test
  void noOverridesLeavePlannedPatternUnchanged() {
    var changes = new StopPatternChanges(Map.of(), Map.of(), Map.of());

    assertThat(changes.deriveStopPattern(plannedPattern, RESOLVES_NOTHING)).isEmpty();
  }

  @Test
  void overrideThatReproducesThePlannedPatternIsNotAModification() {
    // A pickup override that repeats the scheduled value builds a stop pattern identical to the
    // planned one, so the trip must not be reported as running on a modified pattern.
    var changes = new StopPatternChanges(Map.of(0, PickDrop.SCHEDULED), Map.of(), Map.of());

    assertThat(changes.deriveStopPattern(plannedPattern, RESOLVES_NOTHING)).isEmpty();
  }

  @Test
  void overrideThatChangesAPickupIsAModification() {
    var changes = new StopPatternChanges(Map.of(0, PickDrop.NONE), Map.of(), Map.of());

    assertThat(changes.deriveStopPattern(plannedPattern, RESOLVES_NOTHING)).isPresent();
  }

  @Test
  void unresolvableStopReplacementAloneIsNotAModification() {
    // An assigned_stop_id that cannot be resolved is dropped, so an update carrying only such a
    // replacement leaves the trip on its planned pattern.
    var changes = new StopPatternChanges(Map.of(), Map.of(), Map.of(0, "unknown-stop"));

    assertThat(changes.deriveStopPattern(plannedPattern, RESOLVES_NOTHING)).isEmpty();
  }
}
