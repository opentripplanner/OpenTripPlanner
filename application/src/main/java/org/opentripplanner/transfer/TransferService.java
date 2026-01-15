package org.opentripplanner.transfer;

import java.util.Collection;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Access transfers during OTP server runtime. It provides a frozen view of all these elements at a
 * point in time, which is not affected by ongoing transfer updates, allowing results to remain
 * stable over the course of a request.
 */
public interface TransferService {
  /**
   * @param fromStop {@code StopLocation} that is set as a from-stop
   * @return all {@code PathTransfer}s with the specified {@code StopLocation} as a from-stop
   */
  Collection<PathTransfer> findTransfersByStop(StopLocation fromStop);

  /**
   * @param fromStop {@code StopLocation} that is set as a from-stop
   * @return all walk mode {@code PathTransfer}s with the specified {@code StopLocation} as a
   * from-stop
   * @throws UnsupportedOperationException if flex routing is not activated
   * @throws IllegalStateException         if the index was not initialized
   */
  Collection<PathTransfer> findWalkTransfersFromStop(StopLocation fromStop);

  /**
   * @param toStop {@code StopLocation} that is set as a to-stop
   * @return all walk mode {@code PathTransfer}s with the specified {@code StopLocation} as a
   * to-stop
   * @throws UnsupportedOperationException if flex routing is not activated
   * @throws IllegalStateException         if the index was not initialized
   */
  Collection<PathTransfer> findWalkTransfersToStop(StopLocation toStop);
}
