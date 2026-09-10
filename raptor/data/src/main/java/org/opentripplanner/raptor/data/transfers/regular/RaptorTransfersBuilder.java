package org.opentripplanner.raptor.data.transfers.regular;

import gnu.trove.list.array.TIntArrayList;

public final class RaptorTransfersBuilder {
  private final int[] EMPTY_ARRAY = new int[0];

  private final TIntArrayList[] fromStops;
  private final TIntArrayList[] toStops;
  private boolean freeze = false;

  RaptorTransfersBuilder(int nStops) {
    this.fromStops = new TIntArrayList[nStops];
    this.toStops = new TIntArrayList[nStops];
  }

  public RaptorTransfersBuilder addTransfer(int fromStop, int toStop, int duration, int c1) {
    assertOpenForModification();
    addTransfer(fromStops, fromStop, toStop, duration, c1);
    addTransfer(toStops, toStop, fromStop, duration, c1);
    return this;
  }

  private static void addTransfer(TIntArrayList[] a, int sourceStop, int targetStop, int duration, int c1) {
    TIntArrayList list = a[sourceStop];
    if(list == null) {
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

  public RaptorTransfers build() {
    this.freeze = true;
    return new RaptorTransfers(mapToArrays(fromStops), mapToArrays(toStops));
  }

  private int[][] mapToArrays(TIntArrayList[] transfers) {
    int[][] result = new int[transfers.length][];
    for (int i = 0; i < transfers.length; i++) {
      TIntArrayList list = transfers[i];
      result[i] = list == null ? EMPTY_ARRAY : list.toArray();
    }
    return result;
  }
}
