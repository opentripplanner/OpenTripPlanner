package org.opentripplanner.raptor.data.transfers.regular;

import java.io.Serializable;
import java.util.Iterator;
import org.opentripplanner.raptor.spi.RaptorTransfer;

/**
 * Stores transfers as one array of {@link DefaultRaptorTransfer} per stop, avoiding pointer-chasing
 * through a graph of transfer objects on the hot Raptor search path.
 * <p>
 * Note: The returned iterators do not implement the {@code Flyweight} pattern described in
 * {@link org.opentripplanner.raptor.spi.RaptorTransferDataProvider} - each {@link DefaultRaptorTransfer}
 * is a normal, independent object. Raptor does not consume flyweight-style transfer iterators
 * anymore; this can be revisited once that support is added on the routing side.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class RaptorTransferStore implements Serializable {

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
   * @see org.opentripplanner.raptor.spi.RaptorTransferDataProvider#getTransfersFromStop(int)
   */
  public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
    return this.fromStops.getTransfers(fromStop);
  }

  /**
   * @see org.opentripplanner.raptor.spi.RaptorTransferDataProvider#getTransfersToStop(int)
   */
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
    return this.toStops.getTransfers(toStop);
  }

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException(
      "RaptorTransferStore does not support equals() & hashCode()"
    );
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException(
      "RaptorTransferStore does not support equals() & hashCode()"
    );
  }

  @Override
  public String toString() {
    throw new UnsupportedOperationException("RaptorTransfers does not support toString()");
  }
}
