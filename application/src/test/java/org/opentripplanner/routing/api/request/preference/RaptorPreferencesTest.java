package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;

class RaptorPreferencesTest {

  private static final SearchDirection SEARCH_DIRECTION = SearchDirection.REVERSE;
  private static final RaptorProfile PROFILE = RaptorProfile.STANDARD;
  private static final Set<Optimization> OPTIMIZATIONS = Set.of(Optimization.PARALLEL);
  private static final Instant TIME_LIMIT = LocalDate.of(2020, Month.JUNE, 9)
    .atStartOfDay(ZoneIds.UTC)
    .toInstant();

  private static final double RELAX_GENERALIZED_COST_AT_DESTINATION = 1.2;

  private final RaptorPreferences subject = RaptorPreferences.of()
    .withSearchDirection(SEARCH_DIRECTION)
    .withProfile(PROFILE)
    .withOptimizations(OPTIMIZATIONS)
    .withTimeLimit(TIME_LIMIT)
    .withRelaxGeneralizedCostAtDestination(RELAX_GENERALIZED_COST_AT_DESTINATION)
    .build();

  @Test
  void optimizations() {
    assertEquals(OPTIMIZATIONS, subject.optimizations());
  }

  @Test
  void optimizationsShouldNotBeModifiable() {
    assertThrows(UnsupportedOperationException.class, () ->
      subject.optimizations().add(Optimization.PARALLEL)
    );
  }

  @Test
  void optimizationAssetEmptySetOfUsesEnumSetNoneOf() {
    // EnumSet copyOf does not work with empty set, so it needs to be treated as a
    // special case in the builder, using EnumSet.noneOf
    var subject = RaptorPreferences.of().withOptimizations(List.of()).build();
    assertEquals(EnumSet.noneOf(Optimization.class), subject.optimizations());
  }

  @Test
  void optimizationAssetDefault() {
    var subject = RaptorPreferences.of().build();
    assertEquals(
      EnumSet.of(Optimization.PARETO_CHECK_AGAINST_DESTINATION),
      subject.optimizations()
    );
  }

  @Test
  void profile() {
    assertEquals(PROFILE, subject.profile());
  }

  @Test
  void searchDirection() {
    assertEquals(SEARCH_DIRECTION, subject.searchDirection());
  }

  @Test
  void timeLimit() {
    assertEquals(TIME_LIMIT, subject.timeLimit());
  }

  @Test
  void relaxGeneralizedCostAtDestination() {
    // Default is not set (null)
    assertTrue(RaptorPreferences.of().build().relaxGeneralizedCostAtDestination().isEmpty());
    assertEquals(
      RELAX_GENERALIZED_COST_AT_DESTINATION,
      subject.relaxGeneralizedCostAtDestination().orElseThrow()
    );
    assertEquals(
      1.0,
      RaptorPreferences.of()
        .withRelaxGeneralizedCostAtDestination(1.0)
        .build()
        .relaxGeneralizedCostAtDestination()
        .orElseThrow()
    );
    assertThrows(IllegalArgumentException.class, () ->
      RaptorPreferences.of().withRelaxGeneralizedCostAtDestination(0.99).build()
    );
    assertThrows(IllegalArgumentException.class, () ->
      RaptorPreferences.of().withRelaxGeneralizedCostAtDestination(2.01).build()
    );
  }

  @Test
  void testEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(RaptorPreferences.DEFAULT, RaptorPreferences.of().build());
    assertSame(subject, subject.copyOf().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withTimeLimit(null).build();
    var copy = other.copyOf().withTimeLimit(TIME_LIMIT).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("RaptorPreferences{}", RaptorPreferences.DEFAULT.toString());
    assertEquals(
      "RaptorPreferences{" +
      "optimizations: [PARALLEL], " +
      "profile: STANDARD, " +
      "searchDirection: REVERSE, " +
      "timeLimit: 2020-06-09T00:00:00Z, " +
      "relaxGeneralizedCostAtDestination: 1.2" +
      "}",
      subject.toString()
    );
  }
}
