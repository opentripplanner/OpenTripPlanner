package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class TransferPreferencesTest {

  private static final int COST = 200;
  private static final Duration SLACK = Duration.ofSeconds(150);
  private static final double WAIT_RELUCTANCE = 0.95;
  private static final int MAX_TRANSFERS = 17;
  private static final int MAX_ADDITIONAL_TRANSFERS = 7;
  private static final TransferOptimizationPreferences OPTIMIZATION =
    TransferOptimizationPreferences.of().withBackTravelWaitTimeFactor(2.5).build();
  private static final int NONPREFERRED_COST = 30_000;
  private final TransferPreferences subject = TransferPreferences.of()
    .withCost(COST)
    .withSlack(SLACK)
    .withWaitReluctance(WAIT_RELUCTANCE)
    .withMaxTransfers(MAX_TRANSFERS)
    .withMaxAdditionalTransfers(MAX_ADDITIONAL_TRANSFERS)
    .withOptimization(OPTIMIZATION)
    .withNonpreferredCost(NONPREFERRED_COST)
    .build();

  @Test
  void cost() {
    assertEquals(COST, subject.cost());
  }

  @Test
  void slack() {
    assertEquals(SLACK, subject.slack());
  }

  @Test
  void waitReluctance() {
    assertEquals(WAIT_RELUCTANCE, subject.waitReluctance());
  }

  @Test
  void maxTransfers() {
    assertEquals(MAX_TRANSFERS, subject.maxTransfers());
  }

  @Test
  void maxAdditionalTransfers() {
    assertEquals(MAX_ADDITIONAL_TRANSFERS, subject.maxAdditionalTransfers());
  }

  @Test
  void optimization() {
    assertEquals(OPTIMIZATION, subject.optimization());
  }

  @Test
  void nonpreferredCost() {
    assertEquals(NONPREFERRED_COST, subject.nonpreferredCost());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(subject, subject.copyOf().build());
    assertSame(TransferPreferences.DEFAULT, TransferPreferences.of().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withCost(23).build();
    var copy = other.copyOf().withCost(COST).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("TransferPreferences{}", TransferPreferences.DEFAULT.toString());
    assertEquals(
      "TransferPreferences{" +
      "cost: $200, " +
      "slack: 2m30s, " +
      "waitReluctance: 0.95, " +
      "maxTransfers: 17, " +
      "maxAdditionalTransfers: 7, " +
      "optimization: TransferOptimizationPreferences{backTravelWaitTimeFactor: 2.5}, " +
      "nonpreferredCost: $30000" +
      "}",
      subject.toString()
    );
  }
}
