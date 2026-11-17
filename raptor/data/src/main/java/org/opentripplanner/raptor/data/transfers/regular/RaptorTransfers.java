package org.opentripplanner.raptor.data.transfers.regular;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Stores the transfers as one packed {@code int[]} per stop: {@code [stop, duration, c1]} for each
 * transfer. There is no object per transfer, which saves memory (no object header or pointer) and
 * avoids pointer-chasing on the hot Raptor search path. The iterator is a flyweight cursor over the
 * row, see {@link org.opentripplanner.raptor.spi.RaptorTransferDataProvider}.
 * <p>
 * The transfers of each stop are sorted by duration, transfers with the same duration keep their
 * insertion order. The Raptor search stops at the first transfer that is too long, so it relies on
 * this order, see {@code StdRangeRaptorWorkerState}.
 * <p>
 * This class is immutable: neither the outer array nor a row is modified after construction. That
 * makes it safe to share rows between instances, see {@link #withRows(Map)}.
 */
final class RaptorTransfers implements Serializable {

  static final int[] EMPTY_ROW = new int[0];

  /** Number of ints for each transfer in a row: stop, duration and c1. */
  static final int STRIDE = 3;

  private static final int STOP = 0;
  private static final int DURATION = 1;
  private static final int C1 = 2;

  private final int[][] rows;

  /** The rows are sorted by duration, the given rows are sorted in place. */
  RaptorTransfers(int[][] rows) {
    for (var row : rows) {
      sortByDuration(row);
    }
    this.rows = rows;
  }

  private RaptorTransfers(int[][] rows, boolean sorted) {
    this.rows = rows;
  }

  /**
   * The returned iterator is a flyweight, the element returned by {@code next()} is the iterator
   * itself and is only valid until the next call to {@code next()}.
   */
  Iterator<? extends RaptorTransfer> getTransfers(int sourceStop) {
    return new TransferCursor(rows[sourceStop]);
  }

  /** The packed transfers of {@code sourceStop}. The returned array must not be modified. */
  int[] row(int sourceStop) {
    return rows[sourceStop];
  }

  /**
   * Return a copy with the rows of the given source stops replaced. Only the outer array is
   * copied, all other rows are shared with this instance.
   */
  RaptorTransfers withRows(Map<Integer, int[]> rowsBySourceStop) {
    var copy = rows.clone();
    rowsBySourceStop.forEach((sourceStop, row) -> {
      sortByDuration(row);
      copy[sourceStop] = row;
    });
    return new RaptorTransfers(copy, true);
  }

  /** Stable sort, in place, of the packed transfers by duration. */
  private static void sortByDuration(int[] row) {
    int size = row.length / STRIDE;
    if (size < 2) {
      return;
    }
    // Sort (duration, original index) pairs packed in a long, the index gives a stable order
    long[] keys = new long[size];
    for (int i = 0; i < size; ++i) {
      keys[i] = ((long) row[i * STRIDE + DURATION] << 32) | i;
    }
    Arrays.sort(keys);
    int[] sorted = new int[row.length];
    for (int i = 0; i < size; ++i) {
      System.arraycopy(row, (int) keys[i] * STRIDE, sorted, i * STRIDE, STRIDE);
    }
    System.arraycopy(sorted, 0, row, 0, row.length);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RaptorTransfers.class).addNum("size", rows.length).toString();
  }

  /** The flyweight iterator and transfer. */
  private static final class TransferCursor implements Iterator<RaptorTransfer>, RaptorTransfer {

    private final int[] row;
    private int index = -STRIDE;

    TransferCursor(int[] row) {
      this.row = row;
    }

    @Override
    public boolean hasNext() {
      return index + STRIDE < row.length;
    }

    @Override
    public RaptorTransfer next() {
      index += STRIDE;
      return this;
    }

    @Override
    public int stop() {
      return row[index + STOP];
    }

    @Override
    public int durationInSeconds() {
      return row[index + DURATION];
    }

    @Override
    public int c1() {
      return row[index + C1];
    }

    @Override
    public String toString() {
      return asString();
    }
  }
}
