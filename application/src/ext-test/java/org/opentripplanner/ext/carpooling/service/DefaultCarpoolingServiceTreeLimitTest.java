package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.service.DefaultCarpoolingService.MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Unit tests for {@link DefaultCarpoolingService#driverLegTreeLimits}, which sizes the street
 * routing tree at each driver waypoint to its own leg of the route instead of to the whole trip.
 * <p>
 * These guard the properties the rest of the access/egress routing relies on:
 * <ul>
 *   <li>each leg gets {@code pad(leg) + smallestDownstreamBudget} — so a viable, budget-respecting
 *       detour inserted on a leg always stays within the tree, while a stop too far to reach is one
 *       whose detour would breach some downstream stop's budget and be rejected by the delay
 *       constraints anyway;</li>
 *   <li>multi-leg trips produce per-leg limits well below the whole-trip limit — the actual point
 *       of the change;</li>
 *   <li>an incomplete or non-monotonic timeline falls back to whole-trip sizing for every leg.</li>
 * </ul>
 */
class DefaultCarpoolingServiceTreeLimitTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime BASE = LocalDateTime.of(2025, 1, 1, 12, 0).atZone(ZONE);
  private static final WgsCoordinate COORD = new WgsCoordinate(59.9139, 10.7522);
  private static final Duration BUDGET = Duration.ofMinutes(10);

  private static int idCounter = 0;

  /**
   * A leg is padded 50% and its detour allowance — the smallest deviation budget among the stops
   * downstream of the leg — added, matching the production formula.
   */
  private static Duration expectedLimit(Duration leg, Duration allowance) {
    return leg.multipliedBy(3).dividedBy(2).plus(allowance);
  }

  @Test
  void sizesEachLegIndependentlyAndWellBelowWholeTrip() {
    // origin 12:00, intermediate arrives 13:00 (60-min leg), destination 14:40 (100-min leg).
    var trip = trip(
      BASE.plusMinutes(160),
      originStop(BUDGET),
      intermediateStop(BASE.plusMinutes(60), BUDGET),
      destinationStop(BASE.plusMinutes(160), BUDGET)
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    var leg0 = expectedLimit(Duration.ofMinutes(60), BUDGET);
    var leg1 = expectedLimit(Duration.ofMinutes(100), BUDGET);

    // limits[k] sizes the leg from waypoint k to k+1 — waypoint k's forward tree and waypoint k+1's
    // reverse tree. One entry per leg: the origin has no reverse tree, the destination no forward.
    assertEquals(2, limits.length);
    assertEquals(leg0, limits[0]);
    assertEquals(leg1, limits[1]);

    // The whole point of per-leg sizing: each leg is far smaller than the whole-trip span (160 min)
    // would have produced.
    var wholeTrip = expectedLimit(Duration.ofMinutes(160), BUDGET);
    assertTrue(
      limits[0].compareTo(wholeTrip) < 0,
      "per-leg limit should be smaller than the whole-trip limit"
    );
  }

  @Test
  void usesSmallestDownstreamBudgetAsDetourAllowance() {
    // A detour on a leg delays every downstream stop, and each stop is checked against its own
    // deviation budget — so the smallest downstream budget is the most a feasible detour can add.
    // Budgets: origin 50, intermediate 5, destination 40; both legs are 60 min scheduled.
    var trip = trip(
      BASE.plusMinutes(120),
      originStop(Duration.ofMinutes(50)),
      intermediateStop(BASE.plusMinutes(60), Duration.ofMinutes(5)),
      destinationStop(BASE.plusMinutes(120), Duration.ofMinutes(40))
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    // Leg 0 delays both the intermediate stop (5) and the destination (40): allowance min(5, 40)
    // = 5 → pad(60) + 5 = 95. The origin's 50-min budget never participates — no detour can delay
    // the origin.
    assertEquals(Duration.ofMinutes(95), limits[0]);
    // Leg 1 only delays the destination: allowance 40 → pad(60) + 40 = 130. A budget-sized detour
    // (60-min leg + 40-min budget) stays inside the tree; without the allowance term it would not.
    assertEquals(Duration.ofMinutes(130), limits[1]);
  }

  @Test
  void sizesShortLegsWellBelowNearbyStopRadius() {
    // 5-min legs get only their padded duration plus the budget — no nearby-stop-radius floor — so
    // their trees stay small instead of expanding into a region-wide SPT. A stop too far to reach
    // within that limit is one whose detour would breach the budget and be rejected anyway.
    var trip = trip(
      BASE.plusMinutes(10),
      originStop(Duration.ofMinutes(2)),
      intermediateStop(BASE.plusMinutes(5), Duration.ofMinutes(2)),
      destinationStop(BASE.plusMinutes(10), Duration.ofMinutes(2))
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    var leg = expectedLimit(Duration.ofMinutes(5), Duration.ofMinutes(2));
    assertEquals(leg, limits[0]);
    assertEquals(leg, limits[1]);
    assertTrue(
      limits[0].compareTo(MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS) < 0,
      "short legs should not be inflated to the nearby-stop radius"
    );
  }

  @Test
  void usesAimedArrivalWhenExpectedMissing() {
    var trip = trip(
      BASE.plusMinutes(90),
      originStop(BUDGET),
      intermediateAimedStop(BASE.plusMinutes(50), BUDGET),
      destinationStop(BASE.plusMinutes(90), BUDGET)
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    assertEquals(expectedLimit(Duration.ofMinutes(50), BUDGET), limits[0]);
    assertEquals(expectedLimit(Duration.ofMinutes(40), BUDGET), limits[1]);
  }

  @Test
  void destinationLatestArrivalDoesNotInflateLastLeg() {
    // The destination's latest expected arrival is its scheduled arrival plus its deviation
    // budget. The last leg must be sized from the scheduled (expected) arrival — the budget enters
    // the limit exactly once, via the detour allowance, not baked into the leg duration as well.
    var trip = trip(
      BASE.plusMinutes(120),
      originStop(BUDGET),
      intermediateStop(BASE.plusMinutes(60), BUDGET),
      destinationStopWithTimes(BASE.plusMinutes(120), BASE.plusMinutes(160), BUDGET)
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    // Last leg is the scheduled 60 min (12:00+60 → +120), not the 100 min the latest expected
    // arrival (+160) would give.
    assertEquals(expectedLimit(Duration.ofMinutes(60), BUDGET), limits[1]);
  }

  @Test
  void twoWaypointTripSizesTheSingleLegToTheWholeSpan() {
    var trip = trip(
      BASE.plusMinutes(60),
      originStop(BUDGET),
      destinationStop(BASE.plusMinutes(60), BUDGET)
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    var legLimit = expectedLimit(Duration.ofMinutes(60), BUDGET);
    assertEquals(1, limits.length);
    assertEquals(legLimit, limits[0]);
  }

  @Test
  void fallsBackToWholeTripWhenIntermediateTimeMissing() {
    var trip = trip(
      BASE.plusMinutes(120),
      originStop(BUDGET),
      intermediateStop(null, BUDGET),
      destinationStop(BASE.plusMinutes(120), BUDGET)
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    var wholeTrip = expectedLimit(Duration.ofMinutes(120), BUDGET);
    assertEquals(wholeTrip, limits[0]);
    assertEquals(wholeTrip, limits[1]);
  }

  @Test
  void fallsBackToWholeTripWhenDestinationTimeMissing() {
    // The destination is not special-cased: like any other stop, a missing arrival time means the
    // timeline is incomplete and every leg falls back to the whole-trip span.
    var trip = trip(
      BASE.plusMinutes(120),
      originStop(BUDGET),
      intermediateStop(BASE.plusMinutes(60), BUDGET),
      destinationStop(null, BUDGET)
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    var wholeTrip = expectedLimit(Duration.ofMinutes(120), BUDGET);
    assertEquals(wholeTrip, limits[0]);
    assertEquals(wholeTrip, limits[1]);
  }

  @Test
  void fallsBackToWholeTripWhenTimelineNotMonotonic() {
    // Intermediate arrival is before the trip start — an impossible timeline that must not yield a
    // negative leg; the whole-trip span sizes every leg instead.
    var trip = trip(
      BASE.plusMinutes(120),
      originStop(BUDGET),
      intermediateStop(BASE.minusMinutes(10), BUDGET),
      destinationStop(BASE.plusMinutes(120), BUDGET)
    );

    var limits = DefaultCarpoolingService.driverLegTreeLimits(trip);

    var wholeTrip = expectedLimit(Duration.ofMinutes(120), BUDGET);
    assertEquals(wholeTrip, limits[0]);
    assertEquals(wholeTrip, limits[1]);
  }

  private static CarpoolTrip trip(ZonedDateTime endTime, CarpoolStop... stops) {
    return new CarpoolTripBuilder(FeedScopedId.ofNullable("TEST", "trip-" + ++idCounter))
      .withStops(List.of(stops))
      .withTotalCapacity(CarpoolTrip.DEFAULT_TOTAL_CAPACITY)
      .withStartTime(BASE)
      .withEndTime(endTime)
      .build();
  }

  private static CarpoolStop originStop(Duration budget) {
    return CarpoolStop.of(nextId())
      .withCoordinate(COORD)
      .withOnboardCount(1)
      .withDeviationBudget(budget)
      .build();
  }

  private static CarpoolStop destinationStop(@Nullable ZonedDateTime arrival, Duration budget) {
    return intermediateStop(arrival, budget);
  }

  private static CarpoolStop destinationStopWithTimes(
    ZonedDateTime expectedArrival,
    ZonedDateTime latestExpectedArrival,
    Duration budget
  ) {
    return CarpoolStop.of(nextId())
      .withCoordinate(COORD)
      .withOnboardCount(1)
      .withExpectedArrivalTime(expectedArrival)
      .withLatestExpectedArrivalTime(latestExpectedArrival)
      .withDeviationBudget(budget)
      .build();
  }

  private static CarpoolStop intermediateStop(@Nullable ZonedDateTime arrival, Duration budget) {
    return CarpoolStop.of(nextId())
      .withCoordinate(COORD)
      .withOnboardCount(1)
      .withExpectedArrivalTime(arrival)
      .withDeviationBudget(budget)
      .build();
  }

  private static CarpoolStop intermediateAimedStop(ZonedDateTime arrival, Duration budget) {
    return CarpoolStop.of(nextId())
      .withCoordinate(COORD)
      .withOnboardCount(1)
      .withAimedArrivalTime(arrival)
      .withDeviationBudget(budget)
      .build();
  }

  private static FeedScopedId nextId() {
    return FeedScopedId.ofNullable("TEST", "stop-" + ++idCounter);
  }
}
