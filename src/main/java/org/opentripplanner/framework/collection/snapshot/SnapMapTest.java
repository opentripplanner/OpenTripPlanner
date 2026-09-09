package org.opentripplanner.framework.collection.snapshot;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.function.Supplier;
import org.opentripplanner.framework.collection.snapshot.impl.MutableHashSnapMap;

// RUN WITH -ea JVM option to enable assertions
public class SnapMapTest {

  // Number of items in the map. POWER OF TWO ONLY for now.
  private static final int N = 1024 * 1024 * 4;

  // Number of replaced mappings, new mappings, and reads per snapshot publication cycle.
  private static final int N_OVERWRITES = 400;
  private static final int N_NEW_WRITES = 100;
  private static final int N_REMOVALS = 10;
  private static final int N_READS = 1000;

  private interface MapSupplier extends Supplier<MutableSnapMap<String, String>> {}

  public static void main(String[] args) {
    // Use default capacity to test expansion and rehashing.
    // Then use appropriate capacity to see how much faster it is without rehashing.
    MapSupplier jdkSupplierDefaultCapacity = MapAdapter::new;
    MapSupplier jdkSupplierPreparedCapacity = () -> new MapAdapter<>(N);
    MapSupplier customSupplierDefaultCapacity = () -> MutableHashSnapMap.withDefaultCapacity();
    MapSupplier customSupplierPreparedCapacity = () -> MutableHashSnapMap.withInitialCapacity(N);
    MapSupplier mapSupplier = customSupplierDefaultCapacity;
    while (true) {
      simulatedWorkload(mapSupplier);
      oneIteration(mapSupplier);
    }
  }

  private static MutableSnapMap<String, String> makeFilledMutable(
    int nMappings,
    MapSupplier mapSupplier
  ) {
    long start = System.currentTimeMillis();
    MutableSnapMap<String, String> map = mapSupplier.get();
    for (int i = 0; i < nMappings; i++) {
      String key = "K" + i;
      String value = "V" + i;
      map.put(key, value);
    }
    long end = System.currentTimeMillis();
    System.out.println("Initialized mutable map in " + (end - start) + "ms");
    return map;
  }

  public static void oneIteration(MapSupplier mapSupplier) {
    MutableSnapMap<String, String> map = makeFilledMutable(N, mapSupplier);

    // An immutable snapshot instance should be exactly the same size as the mutable one.
    ImmutableSnapMap ss0 = map.snapshot();
    assert map.size() == N;
    assert ss0.size() == N;

    for (int x = 0; x < 10; x++) {
      long start = System.currentTimeMillis();
      // Update mutable map with different values for the same keys, as well as some new keys.
      final int M = N * 2;
      for (int i = 0; i < M; i++) {
        String key = "K" + i;
        String value = "X" + (M - i);
        map.put(key, value);
      }
      ImmutableSnapMap ss1 = map.snapshot();
      assert ss0.size() == N;
      assert ss1.size() == M;
      assert map.size() == M;

      map.put("K1000", "ABCDEF");
      assert ss0.get("K1000").equals("V1000");
      assert ss1.get("K1000").equals("X" + (M - 1000));
      assert map.get("K1000").equals("ABCDEF");

      MutableSnapMap mut = ss0.mutate();
      mut.put("NEWKEY", "NEWVAL");
      assert map.get("NEWKEY") == null;
      assert ss0.get("NEWKEY") == null;
      assert ss1.get("NEWKEY") == null;
      assert mut.get("NEWKEY").equals("NEWVAL");

      ImmutableSnapMap ss2 = mut.snapshot();
      map.put("NEWKEY", "ANOTHERVAL");
      assert ss2.get("NEWKEY").equals("NEWVAL");
      assert ss2.get("NEWKEY").equals(mut.get("NEWKEY"));
      assert ss0.get("NEWKEY") == null;
      assert ss1.get("NEWKEY") == null;
      assert map.get("NEWKEY") == "ANOTHERVAL";
      map.remove("NEWKEY");
      // TODO further testing of remove

      long end = System.currentTimeMillis();
      System.out.println(String.format("Inserted %d elements in %d milliseconds", M, end - start));
    }
  }

  public static void simulatedWorkload(MapSupplier mapSupplier) {
    Random random = new Random();
    long start = System.currentTimeMillis();
    Deque<ImmutableSnapMap> snapshotsInUse = new LinkedList<>();
    // Use default capacity to test expansion and rehashing.
    MutableSnapMap<String, String> buffer = makeFilledMutable(N, mapSupplier);
    long sum = 0;
    for (int i = 0; i <= 500; i++) {
      ImmutableSnapMap<String, String> snapshot = buffer.snapshot();
      snapshotsInUse.add(snapshot);
      if (snapshotsInUse.size() > 3) {
        snapshotsInUse.remove();
      }
      for (int r = 0; r < N_READS; r++) {
        int x = random.nextInt(N);
        String key = "K" + x;
        String existing = snapshot.get(key);
        sum += existing.hashCode(); // Prevent optimization.
        assert existing != null && existing.endsWith(Integer.toString(x)); // Catch both V and OVERWRITE.
      }
      buffer = snapshot.mutate();
      for (int w = 0; w < N_OVERWRITES; w++) {
        int x = random.nextInt(N);
        String key = "K" + x;
        String value = "OVERWRITE" + x;
        buffer.put(key, value);
      }
      for (int w = 0; w < N_NEW_WRITES; w++) {
        int x = random.nextInt(N);
        String key = "NEW KEY " + x;
        String value = "NEW VAL " + x;
        buffer.put(key, value);
      }
      for (int r = 0; r < N_REMOVALS; r++) {
        // FIXME this is removing NEW KEY things that usually don't exist.
        int x = random.nextInt(N);
        String key = "NEW KEY " + x;
        buffer.remove(key);
      }
      long elapsed = System.currentTimeMillis() - start;
      if (i % 10 == 0 && i > 0) {
        System.out.println(String.format("%d iterations, average time %d msec", i, elapsed / i));
      }
    }
  }
}
