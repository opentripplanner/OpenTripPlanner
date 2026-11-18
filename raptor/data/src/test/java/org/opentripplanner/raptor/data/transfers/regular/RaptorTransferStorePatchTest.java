package org.opentripplanner.raptor.data.transfers.regular;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.spi.RaptorTransfer;

class RaptorTransferStorePatchTest {

  private static final int A = 0;
  private static final int B = 1;
  private static final int C = 2;
  private static final int D = 3;
  private static final int N_STOPS = 4;

  /** A -> B, A -> C, B -> C */
  private static RaptorTransferStore original() {
    return RaptorTransferStore.of(N_STOPS)
      .addTransfer(A, B, 100, 10)
      .addTransfer(A, C, 200, 20)
      .addTransfer(B, C, 300, 30)
      .build();
  }

  @Test
  void replaceTheTransfersFromAStop() {
    var patched = original()
      .patch()
      .replaceTransfersFrom(A)
      .addTransfer(A, C, 250, 25)
      .addTransfer(A, D, 400, 40)
      .build();

    assertThat(from(patched, A)).containsExactly(tx(C, 250, 25), tx(D, 400, 40));
    assertThat(from(patched, B)).containsExactly(tx(C, 300, 30));
    // B is no longer a target of A, C gets the new A -> C, and D is a new target of A
    assertThat(to(patched, B)).isEmpty();
    assertThat(to(patched, C)).containsExactly(tx(B, 300, 30), tx(A, 250, 25));
    assertThat(to(patched, D)).containsExactly(tx(A, 400, 40));
  }

  @Test
  void theTransfersOfAPatchedStopAreSortedByDuration() {
    var patched = original()
      .patch()
      .replaceTransfersFrom(A)
      .addTransfer(A, D, 400, 40)
      .addTransfer(A, C, 250, 25)
      .build();

    assertThat(from(patched, A))
      .containsExactly(tx(C, 250, 25), tx(D, 400, 40))
      .inOrder();
    assertThat(to(patched, C))
      .containsExactly(tx(A, 250, 25), tx(B, 300, 30))
      .inOrder();
  }

  @Test
  void aReplacedStopWithoutTransfersHasNone() {
    var patched = original().patch().replaceTransfersFrom(A).build();

    assertThat(from(patched, A)).isEmpty();
    assertThat(to(patched, B)).isEmpty();
    assertThat(to(patched, C)).containsExactly(tx(B, 300, 30));
  }

  @Test
  void addTransfersFromAStopThatHadNone() {
    var patched = original().patch().replaceTransfersFrom(C).addTransfer(C, A, 200, 20).build();

    assertThat(from(patched, C)).containsExactly(tx(A, 200, 20));
    assertThat(to(patched, A)).containsExactly(tx(C, 200, 20));
    assertThat(from(patched, A)).containsExactly(tx(B, 100, 10), tx(C, 200, 20));
  }

  @Test
  void theOriginalStoreIsNotChanged() {
    var original = original();

    original.patch().replaceTransfersFrom(A).addTransfer(A, D, 400, 40).build();

    assertThat(from(original, A)).containsExactly(tx(B, 100, 10), tx(C, 200, 20));
    assertThat(to(original, B)).containsExactly(tx(A, 100, 10));
    assertThat(to(original, D)).isEmpty();
  }

  @Test
  void unaffectedRowsAreShared() {
    var original = original();

    var patched = original.patch().replaceTransfersFrom(A).addTransfer(A, B, 150, 15).build();

    // The transfers from B are not affected by replacing the transfers from A
    assertThat(fromRow(patched, B)).isSameInstanceAs(fromRow(original, B));
    assertThat(fromRow(patched, A)).isNotSameInstanceAs(fromRow(original, A));
  }

  @Test
  void anEmptyPatchReturnsAnEqualStore() {
    var original = original();

    var patched = original.patch().build();

    for (int stop = 0; stop < N_STOPS; ++stop) {
      assertThat(fromRow(patched, stop)).isSameInstanceAs(fromRow(original, stop));
    }
  }

  @Test
  void addATransferFromAStopThatIsNotReplaced() {
    var patch = original().patch();

    assertThrows(IllegalArgumentException.class, () -> patch.addTransfer(A, B, 100, 10));
  }

  @Test
  void aPatchCanOnlyBeBuiltOnce() {
    var patch = original().patch();
    patch.build();

    assertThrows(IllegalStateException.class, patch::build);
    assertThrows(IllegalStateException.class, () -> patch.replaceTransfersFrom(A));
  }

  /**
   * Replace the transfers of random stops, several times in a row, and compare each patched store
   * with a store built from scratch from the same transfers.
   */
  @Test
  void aPatchedStoreHasTheSameTransfersAsAFullBuild() {
    int nStops = 50;
    var random = new Random(42);
    Map<Integer, List<int[]>> transfersByFromStop = new HashMap<>();
    for (int fromStop = 0; fromStop < nStops; ++fromStop) {
      transfersByFromStop.put(fromStop, randomTransfers(random, nStops, fromStop));
    }
    var store = build(nStops, transfersByFromStop);

    for (int round = 0; round < 10; ++round) {
      var replaced = new HashSet<Integer>();
      for (int i = 0; i < 5; ++i) {
        int fromStop = random.nextInt(nStops);
        // Every fourth replaced stop loses all its transfers
        var transfers =
          random.nextInt(4) == 0 ? List.<int[]>of() : randomTransfers(random, nStops, fromStop);
        transfersByFromStop.put(fromStop, transfers);
        replaced.add(fromStop);
      }
      var patch = store.patch();
      for (int fromStop : replaced) {
        patch.replaceTransfersFrom(fromStop);
        for (int[] t : transfersByFromStop.get(fromStop)) {
          patch.addTransfer(fromStop, t[0], t[1], t[2]);
        }
      }
      store = patch.build();

      var expected = build(nStops, transfersByFromStop);
      for (int stop = 0; stop < nStops; ++stop) {
        assertThat(from(store, stop)).containsExactlyElementsIn(from(expected, stop));
        assertThat(to(store, stop)).containsExactlyElementsIn(to(expected, stop));
      }
    }
  }

  private static List<int[]> randomTransfers(Random random, int nStops, int fromStop) {
    List<int[]> transfers = new ArrayList<>();
    for (int toStop = 0; toStop < nStops; ++toStop) {
      if (toStop != fromStop && random.nextInt(8) == 0) {
        transfers.add(new int[] { toStop, random.nextInt(1000), random.nextInt(1000) });
      }
    }
    return transfers;
  }

  private static RaptorTransferStore build(int nStops, Map<Integer, List<int[]>> transfers) {
    var builder = RaptorTransferStore.of(nStops);
    transfers.forEach((fromStop, list) ->
      list.forEach(t -> builder.addTransfer(fromStop, t[0], t[1], t[2]))
    );
    return builder.build();
  }

  private static DefaultRaptorTransfer[] fromRow(RaptorTransferStore store, int stop) {
    return store.fromStops().row(stop);
  }

  private static List<TxEntry> from(RaptorTransferStore store, int stop) {
    return collect(store.getTransfersFromStop(stop));
  }

  private static List<TxEntry> to(RaptorTransferStore store, int stop) {
    return collect(store.getTransfersToStop(stop));
  }

  private static TxEntry tx(int stop, int duration, int c1) {
    return new TxEntry(stop, duration, c1);
  }

  private static List<TxEntry> collect(Iterator<? extends RaptorTransfer> it) {
    List<TxEntry> result = new ArrayList<>();
    while (it.hasNext()) {
      RaptorTransfer t = it.next();
      result.add(new TxEntry(t.stop(), t.durationInSeconds(), t.c1()));
    }
    return result;
  }

  private record TxEntry(int stop, int durationInSeconds, int c1) {}
}
