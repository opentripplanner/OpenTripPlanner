package org.opentripplanner.raptor.data.transfers.regular;

import java.io.Serializable;
import java.util.Iterator;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Stores transfers as one packed {@code int[]} per stop, with no object per transfer, see
 * {@link RaptorTransfers}.
 * <p>
 * The returned iterators are flyweights, as described in
 * {@link org.opentripplanner.raptor.spi.RaptorTransferDataProvider}: the transfer returned by
 * {@code next()} is only valid until the next call to {@code next()}. Use
 * {@link org.opentripplanner.raptor.spi.RaptorTransferValue#copyOf} to keep a transfer.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class RaptorTransferStore implements RaptorRegularTransferService, Serializable {

  private final RaptorTransfers fromStops;
  private final RaptorTransfers toStops;

  RaptorTransferStore(RaptorTransfers fromStops, RaptorTransfers toStops) {
    this.fromStops = fromStops;
    this.toStops = toStops;
  }

  public static RaptorTransferStoreBuilder of(int nStops) {
    return new RaptorTransferStoreBuilder(nStops);
  }

  /**
   * Start a patch of this store: replace the transfers from some stops, keeping all others. The
   * patched store shares all unchanged rows with this one, and this store is not changed.
   */
  public RaptorTransferStorePatch patch() {
    return new RaptorTransferStorePatch(fromStops, toStops);
  }

  /**
   * @see org.opentripplanner.raptor.spi.RaptorTransferDataProvider#getTransfersFromStop(int)
   */
  @Override
  public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
    return this.fromStops.getTransfers(fromStop);
  }

  /**
   * @see org.opentripplanner.raptor.spi.RaptorTransferDataProvider#getTransfersToStop(int)
   */
  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
    return this.toStops.getTransfers(toStop);
  }

  /** The transfers by fromStop, for tests in this package. */
  RaptorTransfers fromStops() {
    return fromStops;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RaptorTransferStore.class)
      .addObj("fromStops", fromStops)
      .addObj("toStops", toStops)
      .toString();
  }
}
