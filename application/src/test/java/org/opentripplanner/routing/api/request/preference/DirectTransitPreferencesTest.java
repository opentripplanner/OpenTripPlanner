package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.DirectTransitPreferences.DEFAULT;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.testfixtures.lang.AssertEqualsAndHashCode;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.transit.model.basic.TransitMode;

class DirectTransitPreferencesTest {

  private static final CostLinearFunction COST_RELAX_FUNCTION = CostLinearFunction.of(
    Cost.ONE_HOUR_WITH_TRANSIT,
    3.0
  );
  private static final double EXTRA_ACCESS_EGRESS_RELUCTANCE = 5.0;
  private static final Duration MAX_ACCESS_EGRESS_DURATION = Duration.ZERO;
  private static final Set<TransitMode> MODES = Set.of(TransitMode.FERRY);

  private DirectTransitPreferences subject = DirectTransitPreferences.of()
    .withEnabled(true)
    .withCostRelaxFunction(COST_RELAX_FUNCTION)
    .withExtraAccessEgressReluctance(EXTRA_ACCESS_EGRESS_RELUCTANCE)
    .withMaxAccessEgressDuration(MAX_ACCESS_EGRESS_DURATION)
    .withModes(MODES)
    .build();

  @Test
  void enabled() {
    assertFalse(DEFAULT.enabled());
    assertTrue(subject.enabled());
  }

  @Test
  void costRelaxFunction() {
    assertEquals(DirectTransitPreferences.DEFAULT_COST_RELAX_FUNCTION, DEFAULT.costRelaxFunction());
    assertEquals(COST_RELAX_FUNCTION, subject.costRelaxFunction());
  }

  @Test
  void extraAccessEgressReluctance() {
    assertEquals(
      DirectTransitPreferences.DEFAULT_RELUCTANCE,
      DEFAULT.extraAccessEgressReluctance()
    );
    assertEquals(EXTRA_ACCESS_EGRESS_RELUCTANCE, subject.extraAccessEgressReluctance());
  }

  @Test
  void maxAccessEgressDuration() {
    assertEquals(Optional.empty(), DEFAULT.maxAccessEgressDuration());
    assertEquals(Optional.of(Duration.ZERO), subject.maxAccessEgressDuration());
  }

  @Test
  void modes() {
    assertEquals(Set.of(), DEFAULT.modes());
    assertFalse(DEFAULT.isModesRestricted());
    assertEquals(MODES, subject.modes());
    assertTrue(subject.isModesRestricted());
  }

  @Test
  void testEqualsAndHashCode() {
    var sameAs = DirectTransitPreferences.of()
      .withEnabled(true)
      .withCostRelaxFunction(COST_RELAX_FUNCTION)
      .withExtraAccessEgressReluctance(EXTRA_ACCESS_EGRESS_RELUCTANCE)
      .withMaxAccessEgressDuration(Duration.ZERO)
      .withModes(MODES)
      .build();

    AssertEqualsAndHashCode.verify(subject).differentFrom(DEFAULT).sameAs(sameAs);
  }

  @Test
  void testToString() {
    assertEquals(
      "DirectTransitPreferences{not enabled}",
      DirectTransitPreferences.DEFAULT.toString()
    );
    assertEquals(
      "DirectTransitPreferences{" +
        "costRelaxFunction: 1h + 3.0 t, " +
        "extraAccessEgressReluctance: 5.0, " +
        "maxAccessEgressDuration: 0s, " +
        "modes: [FERRY]" +
        "}",
      subject.toString()
    );
    // We only want to log "not enabled" if off, the rest of the state
    // is irelevant.
    assertEquals(
      "DirectTransitPreferences{not enabled}",
      subject.copyOf().withEnabled(false).build().toString()
    );
  }
}
