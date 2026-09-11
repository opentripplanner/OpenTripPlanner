package org.opentripplanner.street.geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.strtree.STRtree;

public class HashGridSpatialIndexTest {

  /**
   * We perform a non-regression random test. We insert many random-envelop objects into both a hash
   * grid (OTP) and STRtree (JTS) spatial indexes. We check with many random query that the set of
   * returned objects is the same (after pruning because both could return false positives).
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testHashGridRandom() {
    final double X0 = -0.05;
    final double Y0 = 44.0;
    final double DX = 0.1;
    final double DY = 0.1;
    final int N_OBJS = 1000;
    final int N_QUERIES = 1000;

    Random rand = new Random(42);
    SpatialIndex hashGrid = new HashGridSpatialIndex<>();
    SpatialIndex strTree = new STRtree();

    for (int i = 0; i < N_OBJS; i++) {
      Coordinate a = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
      Coordinate b = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
      DummyObject obj = new DummyObject();
      obj.envelope = new Envelope(a, b);
      hashGrid.insert(obj.envelope, obj);
      strTree.insert(obj.envelope, obj);
    }

    for (int i = 0; i < N_QUERIES; i++) {
      Coordinate a = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
      Coordinate b = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
      Envelope searchEnv = new Envelope(a, b);
      List<DummyObject> hashGridObjs = hashGrid.query(searchEnv);
      // Need to remove non intersecting
      Set<DummyObject> hashGridObjs2 = new HashSet<>();
      for (DummyObject obj : hashGridObjs) {
        if (obj.envelope.intersects(searchEnv)) {
          hashGridObjs2.add(obj);
        }
      }
      List<DummyObject> strtreeObjs = strTree.query(searchEnv);
      // Need to remove non intersecting
      Set<DummyObject> strtreeObjs2 = new HashSet<>();
      for (DummyObject obj : strtreeObjs) {
        if (obj.envelope.intersects(searchEnv)) {
          strtreeObjs2.add(obj);
        }
      }
      boolean equals = hashGridObjs2.equals(strtreeObjs2);
      Assertions.assertTrue(equals);
    }
  }

  /**
   * forEachCandidate must visit exactly the items query() returns, with duplicates only for items
   * stored in more than one of the visited bins.
   */
  @Test
  void forEachCandidateMatchesQuery() {
    Random rand = new Random(7);
    HashGridSpatialIndex<DummyObject> hashGrid = new HashGridSpatialIndex<>();
    for (int i = 0; i < 2000; i++) {
      Coordinate a = new Coordinate(
        rand.nextDouble() * 0.05 + 10.7,
        rand.nextDouble() * 0.05 + 59.9
      );
      Coordinate b = new Coordinate(
        a.x + rand.nextDouble() * 0.004,
        a.y + rand.nextDouble() * 0.006
      );
      DummyObject obj = new DummyObject();
      obj.envelope = new Envelope(a, b);
      hashGrid.insert(obj.envelope, obj);
    }
    for (int i = 0; i < 500; i++) {
      Coordinate a = new Coordinate(
        rand.nextDouble() * 0.05 + 10.7,
        rand.nextDouble() * 0.05 + 59.9
      );
      Envelope searchEnv = new Envelope(a);
      searchEnv.expandBy(rand.nextDouble() * 0.01);
      List<DummyObject> visited = new ArrayList<>();
      Consumer<DummyObject> consumer = visited::add;
      hashGrid.forEachCandidate(searchEnv, consumer);
      Assertions.assertEquals(new HashSet<>(hashGrid.query(searchEnv)), new HashSet<>(visited));
      Assertions.assertTrue(visited.size() >= new HashSet<>(visited).size());
    }
  }

  @Test
  void forEachCandidateVisitsAnItemOncePerBin() {
    HashGridSpatialIndex<String> hashGrid = new HashGridSpatialIndex<>(1.0, 1.0);
    // Bins are centred on integer coordinates (keys are rounded), so this spans bins x=0 and x=1.
    hashGrid.insert(new Envelope(0.2, 0.8, 0.0, 0.0), "spanning");
    hashGrid.insert(new Envelope(0.1, 0.2, 0.0, 0.0), "single");

    List<String> visited = new ArrayList<>();
    hashGrid.forEachCandidate(new Envelope(0.1, 0.2, 0.0, 0.0), visited::add);
    Assertions.assertEquals(List.of("spanning", "single"), visited);

    visited.clear();
    hashGrid.forEachCandidate(new Envelope(0.1, 0.8, 0.0, 0.0), visited::add);
    Assertions.assertEquals(2, visited.stream().filter("spanning"::equals).count());
    Assertions.assertEquals(1, visited.stream().filter("single"::equals).count());
    Assertions.assertEquals(2, hashGrid.query(new Envelope(0.1, 0.8, 0.0, 0.0)).size());
  }

  private static class DummyObject {

    Envelope envelope;

    @Override
    public String toString() {
      return envelope.toString();
    }
  }
}
