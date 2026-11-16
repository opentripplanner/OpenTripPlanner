package org.opentripplanner.raptor.data.transfers.regular;

import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replaces the transfers from a few stops of a {@link RaptorTransferStore}, creating a new store.
 * The cost is proportional to the number of replaced stops and their neighbors, not to the size of
 * the store: the new store shares every row that is not affected with the original one.
 * <p>
 * Each replaced from-stop gets its complete new set of transfers - there is no way to change a
 * single transfer. A replaced from-stop without any added transfer has no transfers afterward.
 * The reverse rows ({@code toStops}) are derived: every stop that was a target of a replaced
 * from-stop before or after the patch gets a rebuilt row.
 * <p>
 * This class is not thread-safe, and can only build one store.
 */
public final class RaptorTransferStorePatch {

  private static final DefaultRaptorTransfer[] EMPTY_ARRAY = new DefaultRaptorTransfer[0];

  private final RaptorTransfers fromStops;
  private final RaptorTransfers toStops;

  /** (toStop, duration, c1) triples by replaced fromStop, in insertion order. */
  private final Map<Integer, TIntArrayList> newTransfersByFromStop = new HashMap<>();
  private boolean built = false;

  RaptorTransferStorePatch(RaptorTransfers fromStops, RaptorTransfers toStops) {
    this.fromStops = fromStops;
    this.toStops = toStops;
  }

  /**
   * Replace all transfers from {@code fromStop}. Add the new ones with {@link #addTransfer};
   * without any, the stop has no transfers in the patched store. Replacing a stop twice is the same
   * as replacing it once, the transfers added before are kept.
   */
  public RaptorTransferStorePatch replaceTransfersFrom(int fromStop) {
    assertNotBuilt();
    newTransfersByFromStop.computeIfAbsent(fromStop, s -> new TIntArrayList(3));
    return this;
  }

  /**
   * Add a transfer from a stop replaced with {@link #replaceTransfersFrom}.
   *
   * @throws IllegalArgumentException if {@code fromStop} is not replaced
   */
  public RaptorTransferStorePatch addTransfer(int fromStop, int toStop, int duration, int c1) {
    assertNotBuilt();
    var transfers = newTransfersByFromStop.get(fromStop);
    if (transfers == null) {
      throw new IllegalArgumentException(
        "Transfers can only be added from a replaced stop, call replaceTransfersFrom(" +
          fromStop +
          ") first."
      );
    }
    transfers.add(toStop);
    transfers.add(duration);
    transfers.add(c1);
    return this;
  }

  public RaptorTransferStore build() {
    assertNotBuilt();
    built = true;
    if (newTransfersByFromStop.isEmpty()) {
      return new RaptorTransferStore(fromStops, toStops);
    }
    return new RaptorTransferStore(
      fromStops.withRows(newFromRows()),
      toStops.withRows(newToRows())
    );
  }

  private Map<Integer, DefaultRaptorTransfer[]> newFromRows() {
    var rows = new HashMap<Integer, DefaultRaptorTransfer[]>();
    newTransfersByFromStop.forEach((fromStop, triples) -> {
      var row = new DefaultRaptorTransfer[triples.size() / 3];
      for (int i = 0; i < row.length; ++i) {
        int j = i * 3;
        row[i] = new DefaultRaptorTransfer(triples.get(j), triples.get(j + 1), triples.get(j + 2));
      }
      rows.put(fromStop, row.length == 0 ? EMPTY_ARRAY : row);
    });
    return rows;
  }

  /**
   * Rebuild the reverse row of every stop that is a target of a replaced from-stop, before or
   * after the patch: drop the transfers from the replaced from-stops, and add their new ones.
   * The transfers from other stops are kept, in their original order.
   */
  private Map<Integer, DefaultRaptorTransfer[]> newToRows() {
    Set<Integer> replaced = newTransfersByFromStop.keySet();

    // The new reverse transfers, by toStop
    var added = new HashMap<Integer, List<DefaultRaptorTransfer>>();
    newTransfersByFromStop.forEach((fromStop, triples) -> {
      for (int j = 0; j < triples.size(); j += 3) {
        added
          .computeIfAbsent(triples.get(j), s -> new ArrayList<>())
          .add(new DefaultRaptorTransfer(fromStop, triples.get(j + 1), triples.get(j + 2)));
      }
    });

    // Old targets of the replaced from-stops, read from the forward rows before the patch
    var affected = new HashSet<>(added.keySet());
    for (int fromStop : replaced) {
      for (var transfer : fromStops.row(fromStop)) {
        affected.add(transfer.stop());
      }
    }

    var rows = new HashMap<Integer, DefaultRaptorTransfer[]>();
    for (int toStop : affected) {
      var row = new ArrayList<DefaultRaptorTransfer>();
      for (var transfer : toStops.row(toStop)) {
        // In a reverse row, stop() is the from-stop of the transfer
        if (!replaced.contains(transfer.stop())) {
          row.add(transfer);
        }
      }
      row.addAll(added.getOrDefault(toStop, List.of()));
      rows.put(toStop, row.isEmpty() ? EMPTY_ARRAY : row.toArray(EMPTY_ARRAY));
    }
    return rows;
  }

  private void assertNotBuilt() {
    if (built) {
      throw new IllegalStateException("The patch is already built");
    }
  }
}
