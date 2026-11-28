package org.opentripplanner.raptor.data.transfers.regular;

import com.carrotsearch.hppc.IntArrayList;

public final class RaptorTransferStoreBuilder {

  private final IntArrayList[] fromStops;
  private final IntArrayList[] toStops;
  private boolean freeze = false;

  RaptorTransferStoreBuilder(int nStops) {
    this.fromStops = new IntArrayList[nStops];
    this.toStops = new IntArrayList[nStops];
  }

  public RaptorTransferStoreBuilder addTransfer(int fromStop, int toStop, int duration, int c1) {
    assertOpenForModification();
    addTransfer(fromStops, fromStop, toStop, duration, c1);
    addTransfer(toStops, toStop, fromStop, duration, c1);
    return this;
  }

  private static void addTransfer(
    IntArrayList[] a,
    int sourceStop,
    int targetStop,
    int duration,
    int c1
  ) {
    IntArrayList list = a[sourceStop];
    if (list == null) {
      list = new IntArrayList(3);
      a[sourceStop] = list;
    }
    list.add(targetStop);
    list.add(duration);
    list.add(c1);
  }

  private void assertOpenForModification() {
    if (freeze) {
      throw new IllegalStateException("Cannot modify frozen transfers");
    }
  }

  public RaptorTransferStore build() {
    this.freeze = true;
    // The rows are created here in sequence, this will make the memory layout more cache-friendly
    // and improve performance.
    return new RaptorTransferStore(mapToArrays(fromStops), mapToArrays(toStops));
  }

  private RaptorTransfers mapToArrays(IntArrayList[] transfers) {
    int[][] result = new int[transfers.length][];
    for (int i = 0; i < transfers.length; i++) {
      result[i] = transfers[i] == null ? RaptorTransfers.EMPTY_ROW : transfers[i].toArray();
    }
    return new RaptorTransfers(result);
  }
}
