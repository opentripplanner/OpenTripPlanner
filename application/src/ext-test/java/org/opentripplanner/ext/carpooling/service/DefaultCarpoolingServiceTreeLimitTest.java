package org.opentripplanner.ext.carpooling.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.opentripplanner.ext.carpooling.routing.DriverLegLimits;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Unit tests for {@link DriverLegLimits#legLimits}: each leg gets
 * {@code leg + slack + smallestDownstreamBudget} capped at {@link CarpoolTrip#MAX_TRIP_DURATION},
 * and multi-leg trips get per-leg limits well below the whole-trip limit. The leg durations are
 * passed in explicitly here; in production they are OTP's own routed durations (see
 * {@link org.opentripplanner.ext.carpooling.routing.CorridorBuilder}).
 */
class DefaultCarpoolingServiceTreeLimitTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Oslo");
  private static final ZonedDateTime BASE = LocalDateTime.of(2025, 1, 1, 12, 0).atZone(ZONE);
  private static final WgsCoordinate COORD = new WgsCoordinate(59.9139, 10.7522);
  private static final Duration BUDGET = Duration.ofMinutes(10);

  private static int idCounter = 0;

  /** Mirrors the small slack {@link DriverLegLimits#legLimits} adds per leg. */
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

    var limits = DriverLegLimits.legLimits(trip, new Duration[] {
      Duration.ofMinutes(60),
      Duration.ofMinutes(100),
    });

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

    var limits = DriverLegLimits.legLimits(trip, new Duration[] {
      Duration.ofMinutes(5),
      Duration.ofMinutes(5),
    });

    var leg = expectedLimit(Duration.ofMinutes(5), Duration.ofMinutes(2));
    assertEquals(leg, limits[0]);
    assertEquals(leg, limits[1]);
    assertTrue(
      limits[0].compareTo(Duration.ofMinutes(60)) < 0,
      "short legs should not be inflated to the former nearby-stop radius"
    );
  }

  @Test
  void twoWaypointTripSizesTheSingleLeg() {
    var trip = trip(stop(BUDGET), stop(BUDGET));

    var limits = DriverLegLimits.legLimits(trip, new Duration[] { Duration.ofMinutes(60) });

    assertEquals(1, limits.length);
    assertEquals(expectedLimit(Duration.ofMinutes(60), BUDGET), limits[0]);
  }

  @Test
  void capsEachLegLimitAtMaxTripDuration() {
    // An inconsistent trip whose leg travel time already exceeds the trip cap (e.g. one that
    // slipped past the mapper) must not size a tree beyond MAX_TRIP_DURATION; otherwise a single
    // request could expand a multi-hour street tree.
    var trip = trip(stop(BUDGET), stop(BUDGET));

    var limits = DriverLegLimits.legLimits(trip, new Duration[] { Duration.ofHours(4) });

    assertEquals(CarpoolTrip.MAX_TRIP_DURATION, limits[0]);
  }

  /**
   * The passenger's forward tree must reach {@code legLimit - beeline(legStart, passenger)} for
   * the widest leg: the driver first has to get from the leg start to the passenger, which takes
   * at least the beeline at the fastest speed, before the part the tree covers begins.
   */
  @Test
  void passengerForwardTreeLimitSubtractsTheBeelineFromTheLegStart() {
    double maxSpeed = 40.0;
    var legStart = vertex("a0", COORD);
    var legEnd = vertex("a1", COORD.moveEastMeters(20_000));
    var passenger = vertex("p", COORD.moveEastMeters(4_000));
    var legLimits = new Duration[] { Duration.ofMinutes(30) };

    var limit = DefaultCarpoolingService.passengerTreeLimit(
      passenger,
      List.of(legStart, legEnd),
      List.of(legLimits),
      maxSpeed,
      true
    );

    // 4 km at 40 m/s is 100 s the driver needs before reaching the passenger. The test
    // coordinates are placed with a metres-to-degrees approximation, hence the tolerance.
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(100), limit);
  }

  @Test
  void passengerReverseTreeLimitSubtractsTheBeelineToTheLegEnd() {
    double maxSpeed = 40.0;
    var legStart = vertex("a0", COORD);
    var legEnd = vertex("a1", COORD.moveEastMeters(20_000));
    var passenger = vertex("p", COORD.moveEastMeters(12_000));
    var legLimits = new Duration[] { Duration.ofMinutes(30) };

    var limit = DefaultCarpoolingService.passengerTreeLimit(
      passenger,
      List.of(legStart, legEnd),
      List.of(legLimits),
      maxSpeed,
      false
    );

    // 8 km at 40 m/s is 200 s the driver still needs after the passenger.
    assertWithinTwoSeconds(Duration.ofMinutes(30).minusSeconds(200), limit);
  }

  /** The widest leg decides, and a passenger far beyond every leg's reach yields zero, not negative. */
  @Test
  void passengerTreeLimitsTakeTheWidestLegAndFloorAtZero() {
    double maxSpeed = 40.0;
    var a0 = vertex("a0", COORD);
    var a1 = vertex("a1", COORD.moveEastMeters(1_000));
    var a2 = vertex("a2", COORD.moveEastMeters(30_000));
    var near = vertex("near", COORD.moveEastMeters(2_000));
    var legLimits = new Duration[] { Duration.ofMinutes(2), Duration.ofMinutes(40) };

    var forward = DefaultCarpoolingService.passengerTreeLimit(
      near,
      List.of(a0, a1, a2),
      List.of(legLimits),
      maxSpeed,
      true
    );
    // Leg 0: 2 min - 50 s; leg 1: 40 min - 25 s. The second is wider.
    assertWithinTwoSeconds(Duration.ofMinutes(40).minusSeconds(25), forward);

    var far = vertex("far", COORD.moveEastMeters(200_000));
    assertEquals(
      Duration.ZERO,
      DefaultCarpoolingService.passengerTreeLimit(
        far,
        List.of(a0, a1, a2),
        List.of(legLimits),
        maxSpeed,
        true
      )
    );
  }

  private static void assertWithinTwoSeconds(Duration expected, Duration actual) {
    assertTrue(
      Math.abs(expected.minus(actual).toSeconds()) <= 2,
      "expected about " + expected + " but was " + actual
    );
  }

  private static Vertex vertex(String label, WgsCoordinate coordinate) {
    return new SimpleVertex(label, coordinate.latitude(), coordinate.longitude());
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
