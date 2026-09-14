package org.opentripplanner.transit.transfer.regular;

import org.opentripplanner.raptor.spi.RaptorTransfer;

import java.util.Iterator;

/**
 * TODO Migrate this into the Raptor SPI. This interface should be part of the Raptor SPI, and the
 *      {@code RaptorTransferDataProvider} should provide the instance directly. This should be
 *      done once the old regular-transfer pipeline is fully removed.
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
