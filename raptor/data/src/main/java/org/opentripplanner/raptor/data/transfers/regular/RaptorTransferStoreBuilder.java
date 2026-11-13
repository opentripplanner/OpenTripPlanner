package org.opentripplanner.raptor.data.transfers.regular;

import gnu.trove.list.array.TIntArrayList;

public final class RaptorTransferStoreBuilder {

  private final DefaultRaptorTransfer[] EMPTY_ARRAY = new DefaultRaptorTransfer[0];

  private final TIntArrayList[] fromStops;
  private final TIntArrayList[] toStops;
  private boolean freeze = false;

  RaptorTransferStoreBuilder(int nStops) {
    this.fromStops = new TIntArrayList[nStops];
    this.toStops = new TIntArrayList[nStops];
  }

  public RaptorTransferStoreBuilder addTransfer(int fromStop, int toStop, int duration, int c1) {
    assertOpenForModification();
    addTransfer(fromStops, fromStop, toStop, duration, c1);
    addTransfer(toStops, toStop, fromStop, duration, c1);
    return this;
  }

  private static void addTransfer(
    TIntArrayList[] a,
    int sourceStop,
    int targetStop,
    int duration,
    int c1
  ) {
    TIntArrayList list = a[sourceStop];
    if (list == null) {
      list = new TIntArrayList(3);
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
    // The arrays/DefaultTransfer are created here in sequence, this will make the memory layout
    // more cache-friendly and improve performance.
    return new RaptorTransferStore(mapToArrays(fromStops), mapToArrays(toStops));
  }

  private RaptorTransfers mapToArrays(TIntArrayList[] transfers) {
    DefaultRaptorTransfer[][] result = new DefaultRaptorTransfer[transfers.length][];
    for (int i = 0; i < transfers.length; i++) {
      result[i] = mapToArray(transfers[i]);
    }
    return new RaptorTransfers(result);
  }

  private DefaultRaptorTransfer[] mapToArray(TIntArrayList transfers) {
    if (transfers == null) {
      return EMPTY_ARRAY;
    }
    int[] array = transfers.toArray();
    int size = array.length / 3;

    DefaultRaptorTransfer[] result = new DefaultRaptorTransfer[size];
    for (int i = 0; i < size; ++i) {
      int j = i * 3;
      result[i] = new DefaultRaptorTransfer(array[j], array[j + 1], array[j + 2]);
    }
    return result;
  }
}
