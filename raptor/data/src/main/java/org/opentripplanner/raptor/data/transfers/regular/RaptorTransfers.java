package org.opentripplanner.raptor.data.transfers.regular;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Stores transfers as one array of {@link DefaultRaptorTransfer} per stop, avoiding pointer-chasing
 * through a graph of transfer objects on the hot Raptor search path.
 * <p>
 * Benchmarked ~3.5-4x faster than the legacy object-reference index (~3-4 ns/op vs ~13 ns/op) —
 * the win is cache locality from one packed array per direction, not the array layout itself.
 * <p>
 * The transfers of each stop are sorted by duration. The Raptor search stops at the first transfer
 * that is too long, so it relies on this order, see {@code StdRangeRaptorWorkerState}.
 * <p>
 * This class is immutable: neither the outer array nor a row is modified after construction. That
 * makes it safe to share rows between instances, see {@link #withRows(Map)}.
 */
final class RaptorTransfers implements Serializable {

  private static final Comparator<DefaultRaptorTransfer> BY_DURATION = Comparator.comparingInt(
    DefaultRaptorTransfer::durationInSeconds
  );

  private final DefaultRaptorTransfer[][] transfers;

  /** The rows are sorted by duration, the given rows are sorted in place. */
  RaptorTransfers(DefaultRaptorTransfer[][] transfers) {
    for (var row : transfers) {
      sortByDuration(row);
    }
    this.transfers = transfers;
  }

  private RaptorTransfers(DefaultRaptorTransfer[][] transfers, boolean sorted) {
    this.transfers = transfers;
  }

  Iterator<? extends RaptorTransfer> getTransfers(int sourceStop) {
    return new LightweightTransferIterator(this.transfers[sourceStop]);
  }

  /** The transfers of {@code sourceStop}. The returned array must not be modified. */
  DefaultRaptorTransfer[] row(int sourceStop) {
    return transfers[sourceStop];
  }

  /**
   * Return a copy with the rows of the given source stops replaced. Only the outer array is
   * copied, all other rows are shared with this instance.
   */
  RaptorTransfers withRows(Map<Integer, DefaultRaptorTransfer[]> rowsBySourceStop) {
    var copy = transfers.clone();
    rowsBySourceStop.forEach((sourceStop, row) -> {
      sortByDuration(row);
      copy[sourceStop] = row;
    });
    return new RaptorTransfers(copy, true);
  }

  private static void sortByDuration(DefaultRaptorTransfer[] row) {
    Arrays.sort(row, BY_DURATION);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RaptorTransfers.class).addNum("size", transfers.length).toString();
  }

  private static final class LightweightTransferIterator implements Iterator<RaptorTransfer> {

    private final DefaultRaptorTransfer[] a;
    private int index;

    LightweightTransferIterator(DefaultRaptorTransfer[] a) {
      this.a = a;
      this.index = this.a.length == 0 ? 0 : -1;
    }

    @Override
    public boolean hasNext() {
      index += 1;
      return index < a.length;
    }

    @Override
    public RaptorTransfer next() {
      return a[index];
    }
  }
}
