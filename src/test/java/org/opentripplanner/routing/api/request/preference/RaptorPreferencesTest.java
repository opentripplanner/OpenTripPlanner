package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.spi.SearchDirection;

class RaptorPreferencesTest {

  private static final SearchDirection SEARCH_DIRECTION = SearchDirection.REVERSE;
  private static final RaptorProfile PROFILE = RaptorProfile.STANDARD;
  private static final Set<Optimization> OPTIMIZATIONS = Set.of(Optimization.PARALLEL);
  private static final Instant TIME_LIMIT = LocalDate
    .of(2020, Month.JUNE, 9)
    .atStartOfDay(ZoneIds.UTC)
    .toInstant();

  private final RaptorPreferences subject = RaptorPreferences
    .of()
    .withSearchDirection(SEARCH_DIRECTION)
    .withProfile(PROFILE)
    .withOptimizations(OPTIMIZATIONS)
    .withTimeLimit(TIME_LIMIT)
    .build();

  @Test
  void optimizations() {
    assertEquals(OPTIMIZATIONS, subject.optimizations());
  }

  @Test
  void optimizationsShouldNotBeModifiable() {
    assertThrows(
      UnsupportedOperationException.class,
      () -> subject.optimizations().add(Optimization.PARALLEL)
    );
  }

  @Test
  void getProfile() {
    assertEquals(PROFILE, subject.profile());
  }

  @Test
  void getSearchDirection() {
    assertEquals(SEARCH_DIRECTION, subject.searchDirection());
  }

  @Test
  void getTimeLimit() {
    assertEquals(TIME_LIMIT, subject.timeLimit());
  }

  @Test
  void testEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(RaptorPreferences.DEFAULT, RaptorPreferences.of().build());
    assertSame(subject, subject.copyOf().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withTimeLimit(null).build();
    var copy = other.copyOf().withTimeLimit(TIME_LIMIT).build();
    assertEqualsAndHashCode(StreetPreferences.DEFAULT, subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("RaptorPreferences{}", RaptorPreferences.DEFAULT.toString());
    assertEquals(
      "RaptorPreferences{" +
      "optimizations: [PARALLEL], " +
      "profile: STANDARD, " +
      "searchDirection: REVERSE, " +
      "timeLimit: 2020-06-09T00:00:00Z" +
      "}",
      subject.toString()
    );
  }
}
