package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * The fast evaluation must choose exactly what the exhaustive one did: for random trips, budgets,
 * capacities and driving times, every pair of insertion positions is evaluated the old way (build
 * the whole modified route, sum it up, check every stop's budget and the capacity) and the first
 * pair with the smallest total trip duration must be the evaluator's answer.
 */
class InsertionEvaluatorOracleTest {

  private static final WgsCoordinate ORIGIN = new WgsCoordinate(63.43, 10.39);

  /** Driving times from a seed: deterministic per (from, to), a few pairs unroutable. */
  private static final class FakeRouter implements CarpoolRouter {

    private final long seed;

    FakeRouter(long seed) {
      this.seed = seed;
    }

    @Override
    @Nullable
    public RoutedSegment route(Vertex from, Vertex to) {
      long h = seed * 31 + from.getLabelString().hashCode() * 17L + to.getLabelString().hashCode();
      h ^= h >>> 13;
      int bucket = (int) Math.floorMod(h, 23L);
      if (bucket == 0) {
        return null;
      }
      int seconds = 60 + 30 * bucket;
      return new FixedSegment(from, to, seconds);
    }
  }

  private record FixedSegment(
    Vertex from,
    Vertex to,
    int durationSeconds
  ) implements RoutedSegment {
    @Override
    public org.opentripplanner.astar.model.GraphPath<
      org.opentripplanner.street.search.state.State,
      org.opentripplanner.street.model.edge.Edge,
      Vertex
    > path() {
      throw new UnsupportedOperationException("durations only");
    }
  }

  private static Vertex vertex(String label) {
    return new SimpleVertex(label, ORIGIN.latitude(), ORIGIN.longitude());
  }

  private static CarpoolTripWithVertices randomTrip(Random rng, int stops) {
    var start = ZonedDateTime.parse("2030-01-01T08:00:00+01:00");
    var tripId = new FeedScopedId("T", "trip-" + rng.nextInt(1_000_000));
    var stopList = new ArrayList<CarpoolStop>();
    var vertices = new ArrayList<Vertex>();
    int capacity = 2 + rng.nextInt(3);
    for (int k = 0; k < stops; k++) {
      stopList.add(
        CarpoolStop.of(new FeedScopedId("T", tripId.getId() + "-s" + k))
          .withCoordinate(ORIGIN.moveEastMeters(1000.0 * k))
          .withOnboardCount(k == stops - 1 ? 0 : rng.nextInt(capacity + 1))
          .withDeviationBudget(Duration.ofMinutes(1 + rng.nextInt(20)))
          .build()
      );
      vertices.add(vertex(tripId.getId() + "-v" + k));
    }
    var trip = new CarpoolTripBuilder(tripId)
      .withStops(stopList)
      .withTotalCapacity(capacity)
      .withStartTime(start)
      .withEndTime(start.plusHours(1))
      .build();
    return new CarpoolTripWithVertices(trip, vertices);
  }

  /** The old evaluation: every pair, the whole route rebuilt, first minimum wins. */
  @Nullable
  private static int[] oracle(
    CarpoolTripWithVertices tripWithVertices,
    CarpoolRouter router,
    Vertex pickup,
    Vertex dropoff,
    Duration stopDuration
  ) {
    var points = tripWithVertices.vertices();
    var trip = tripWithVertices.trip();
    int n = points.size();
    var baseline = new ArrayList<RoutedSegment>();
    for (int k = 0; k < n - 1; k++) {
      var seg = router.route(points.get(k), points.get(k + 1));
      if (seg == null) {
        return null;
      }
      baseline.add(seg);
    }
    Duration[] original = RoutedSegment.cumulativeDurations(baseline, stopDuration);
    long bestTotal = Long.MAX_VALUE;
    int[] best = null;
    for (int p = 1; p < n; p++) {
      for (int d = p + 1; d <= n; d++) {
        if (!trip.hasCapacityForInsertion(p, d, 1)) {
          continue;
        }
        var modifiedPoints = new ArrayList<>(points);
        modifiedPoints.add(p, pickup);
        modifiedPoints.add(d, dropoff);
        var segments = new ArrayList<RoutedSegment>();
        boolean routable = true;
        for (int k = 0; k < modifiedPoints.size() - 1 && routable; k++) {
          var seg = router.route(modifiedPoints.get(k), modifiedPoints.get(k + 1));
          if (seg == null) {
            routable = false;
          } else {
            segments.add(seg);
          }
        }
        if (!routable) {
          continue;
        }
        Duration[] modified = RoutedSegment.cumulativeDurations(segments, stopDuration);
        if (!withinBudgets(original, modified, p, d, trip.stops())) {
          continue;
        }
        long total = modified[modified.length - 1].toSeconds();
        if (total < bestTotal) {
          bestTotal = total;
          best = new int[] { p, d, (int) total };
        }
      }
    }
    return best;
  }

  /** Every existing stop but the origin is delayed by at most its own deviation budget. */
  private static boolean withinBudgets(
    Duration[] original,
    Duration[] modified,
    int pickupPos,
    int dropoffPos,
    List<CarpoolStop> stops
  ) {
    for (int k = 1; k < original.length; k++) {
      int shifted = k >= pickupPos ? k + 1 : k;
      shifted = shifted >= dropoffPos ? shifted + 1 : shifted;
      var delay = modified[shifted].minus(original[k]);
      if (delay.compareTo(stops.get(k).getDeviationBudget()) > 0) {
        return false;
      }
    }
    return true;
  }

  @Test
  void agreesWithTheExhaustiveEvaluationOnRandomTrips() {
    var rng = new Random(20260918);
    int compared = 0;
    int withInsertion = 0;
    for (int round = 0; round < 400; round++) {
      int stops = 2 + rng.nextInt(6);
      var tripWithVertices = randomTrip(rng, stops);
      var router = new FakeRouter(rng.nextLong());
      var stopDuration = Duration.ofSeconds(rng.nextBoolean() ? 0 : 60);
      var pickup = vertex("passenger-pickup-" + round);
      var dropoff = vertex("passenger-dropoff-" + round);

      var expected = oracle(tripWithVertices, router, pickup, dropoff, stopDuration);
      var actual = new InsertionEvaluator(router, stopDuration).findBestInsertion(
        tripWithVertices,
        new PassengerSnap(pickup, dropoff, null, null)
      );

      compared++;
      if (expected == null) {
        assertNull(actual, "round " + round + ": the oracle finds no insertion");
        continue;
      }
      withInsertion++;
      assertNotNull(actual, "round " + round);
      assertEquals(expected[0], actual.pickupPosition(), "round " + round + " pickup position");
      assertEquals(expected[1], actual.dropoffPosition(), "round " + round + " dropoff position");
      assertEquals(
        expected[2],
        actual.totalTripDuration().toSeconds(),
        "round " + round + " total duration"
      );
    }
    assertEquals(400, compared);
    // The generator has to produce a healthy share of both outcomes for the test to mean much.
    org.junit.jupiter.api.Assertions.assertTrue(
      withInsertion > 100 && withInsertion < 380,
      "insertions found in " + withInsertion + " of 400 rounds"
    );
  }
}
