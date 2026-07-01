package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.service.DefaultCarpoolingService.MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Unit tests for {@link DefaultCarpoolingService#driverLegTreeLimits}: each leg gets
 * {@code leg + slack + smallestDownstreamBudget} capped at {@link CarpoolTrip#MAX_TRIP_DURATION},
 * and multi-leg trips get per-leg limits well below the whole-trip limit. The leg durations are
 * passed in explicitly here; in production they are OTP's own routed durations (see
 * {@link DefaultCarpoolingService#resolveLegDurations}).
 */
class DefaultCarpoolingServiceTreeLimitTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime BASE = LocalDateTime.of(2025, 1, 1, 12, 0).atZone(ZONE);
  private static final WgsCoordinate COORD = new WgsCoordinate(59.9139, 10.7522);
  private static final Duration BUDGET = Duration.ofMinutes(10);

  private static int idCounter = 0;

  /** Mirrors the small slack {@link DefaultCarpoolingService#driverLegTreeLimits} adds per leg. */
  private static final Duration SLACK = Duration.ofMinutes(1);

  /** Matches the production formula: leg + flat slack + smallest downstream deviation budget. */
  private static Duration expectedLimit(Duration leg, Duration allowance) {
    return leg.plus(SLACK).plus(allowance);
  }

  @Test
  void sizesEachLegFromItsDurationAndSmallestDownstreamBudget() {
    // Budgets: origin 50, intermediate 5, destination 40. The origin's budget never participates —
    // no detour can delay the origin.
    var trip = trip(
      stop(Duration.ofMinutes(50)),
      stop(Duration.ofMinutes(5)),
      stop(Duration.ofMinutes(40))
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(
      trip,
      new Duration[] { Duration.ofMinutes(60), Duration.ofMinutes(100) }
    );

    assertEquals(2, limits.length);
    // Leg 0 allowance = min(downstream budgets) = min(5, 40) = 5.
    assertEquals(expectedLimit(Duration.ofMinutes(60), Duration.ofMinutes(5)), limits[0]);
    // Leg 1 only delays the destination: allowance 40.
    assertEquals(expectedLimit(Duration.ofMinutes(100), Duration.ofMinutes(40)), limits[1]);

    // Even the larger per-leg limit stays well below a whole-trip tree (160 min of travel).
    assertTrue(
      limits[1].compareTo(expectedLimit(Duration.ofMinutes(160), Duration.ofMinutes(40))) < 0,
      "per-leg limits should be smaller than a whole-trip limit"
    );
  }

  @Test
  void sizesShortLegsWellBelowNearbyStopRadius() {
    // 5-min legs get only duration + slack + budget — no nearby-stop-radius floor — so their
    // trees stay small.
    var trip = trip(
      stop(Duration.ofMinutes(2)),
      stop(Duration.ofMinutes(2)),
      stop(Duration.ofMinutes(2))
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(
      trip,
      new Duration[] { Duration.ofMinutes(5), Duration.ofMinutes(5) }
    );

    var leg = expectedLimit(Duration.ofMinutes(5), Duration.ofMinutes(2));
    assertEquals(leg, limits[0]);
    assertEquals(leg, limits[1]);
    assertTrue(
      limits[0].compareTo(MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS) < 0,
      "short legs should not be inflated to the nearby-stop radius"
    );
  }

  @Test
  void twoWaypointTripSizesTheSingleLeg() {
    var trip = trip(stop(BUDGET), stop(BUDGET));

    var limits = DefaultCarpoolingService.driverLegTreeLimits(
      trip,
      new Duration[] { Duration.ofMinutes(60) }
    );

    assertEquals(1, limits.length);
    assertEquals(expectedLimit(Duration.ofMinutes(60), BUDGET), limits[0]);
  }

  @Test
  void capsEachLegLimitAtMaxTripDuration() {
    // An inconsistent trip whose leg travel time already exceeds the trip cap (e.g. one that
    // slipped past the mapper) must not size a tree beyond MAX_TRIP_DURATION; otherwise a single
    // request could expand a multi-hour street tree.
    var trip = trip(stop(BUDGET), stop(BUDGET));

    var limits = DefaultCarpoolingService.driverLegTreeLimits(
      trip,
      new Duration[] { Duration.ofHours(4) }
    );

    assertEquals(CarpoolTrip.MAX_TRIP_DURATION, limits[0]);
  }

  private static CarpoolTrip trip(CarpoolStop... stops) {
    return new CarpoolTripBuilder(FeedScopedId.ofNullable("TEST", "trip-" + ++idCounter))
      .withStops(List.of(stops))
      .withTotalCapacity(CarpoolTrip.DEFAULT_TOTAL_CAPACITY)
      .withStartTime(BASE)
      .withEndTime(BASE.plusHours(3))
      .build();
  }

  private static CarpoolStop stop(Duration budget) {
    return CarpoolStop.of(nextId())
      .withCoordinate(COORD)
      .withOnboardCount(1)
      .withDeviationBudget(budget)
      .build();
  }

  private static FeedScopedId nextId() {
    return FeedScopedId.ofNullable("TEST", "stop-" + ++idCounter);
  }
}
