package org.opentripplanner.transit.transfer.regular;

import org.opentripplanner.raptor.spi.RaptorTransfer;

import java.util.Iterator;

/**
 * TODO TX - This interface should be replaced by the Raptor SPI
 *           {@link org.opentripplanner.raptor.spi.RaptorTransferDataProvider}. But, we need to move constrained
 *           transfers into this module first. This needs to be analyzed - not necessarily the right thing.
 *
 * @see org.opentripplanner.raptor.spi.RaptorTransferDataProvider
 */
public interface RaptorRegularTransferService {
  /**
   * @see org.opentripplanner.raptor.spi.RaptorTransferDataProvider#getTransfersFromStop(int)
   */
  Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop);

  /**
   * @see org.opentripplanner.raptor.spi.RaptorTransferDataProvider#getTransfersToStop(int)
   */
  Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop);
}
