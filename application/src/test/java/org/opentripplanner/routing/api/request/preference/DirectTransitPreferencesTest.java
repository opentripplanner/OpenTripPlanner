package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.DirectTransitPreferences.DEFAULT;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

class DirectTransitPreferencesTest {

  private static final CostLinearFunction COST_RELAX_FUNCTION = CostLinearFunction.of(
    Cost.ONE_HOUR_WITH_TRANSIT,
    3.0
  );
  private static final double EXTRA_ACCESS_EGRESS_RELUCTANCE = 5.0;
  private static final Duration MAX_ACCESS_EGRESS_DURATION = Duration.ZERO;

  private DirectTransitPreferences subject = DirectTransitPreferences.of()
    .withEnabled(true)
    .withCostRelaxFunction(COST_RELAX_FUNCTION)
    .withExtraAccessEgressReluctance(EXTRA_ACCESS_EGRESS_RELUCTANCE)
    .withMaxAccessEgressDuration(MAX_ACCESS_EGRESS_DURATION)
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
  void testEqualsAndHashCode() {
    var sameAs = DirectTransitPreferences.of()
      .withEnabled(true)
      .withCostRelaxFunction(COST_RELAX_FUNCTION)
      .withExtraAccessEgressReluctance(EXTRA_ACCESS_EGRESS_RELUCTANCE)
      .withMaxAccessEgressDuration(Duration.ZERO)
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
        "maxAccessEgressDuration: 0s" +
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
