package org.opentripplanner.raptor.rangeraptor.destination;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorTransferDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Add a transfer leg to the {@link PathBuilder} for a transfer arrival. The transfer used is not
 * kept in the Raptor state, so the duration and the cost are restored here:
 * <ul>
 *   <li>The duration is the time between the two arrivals, minus the wait time added by a via
 *       visit, see {@link ArrivalView#viaWaitTime()}.</li>
 *   <li>The cost is the c1 difference between the two arrivals if c1 is part of the state (McRaptor).
 *       Otherwise, it is looked up in the transfer data.</li>
 *   <li>The transfer of a via connection is kept in the state and used as is.</li>
 * </ul>
 */
final class TransferLegMapper<T extends RaptorTripSchedule> {

  private final RaptorTransferDataProvider<T> transferData;

  TransferLegMapper(RaptorTransferDataProvider<T> transferData) {
    this.transferData = transferData;
  }

  /**
   * @param fromStop the stop the transfer starts at, in the direction from origin to destination.
   * @param toStop the stop the transfer arrives at, in the direction from origin to destination.
   */
  void addTransfer(
    PathBuilder<T> pathBuilder,
    ArrivalView<T> transferArrival,
    int fromStop,
    int toStop
  ) {
    var viaTransfer = transferArrival.viaTransfer();
    if (viaTransfer != null) {
      pathBuilder.viaTransfer(viaTransfer, toStop);
      return;
    }
    int duration = transferDuration(transferArrival);
    int c1 = transferArrival.transferC1();

    if (c1 == RaptorConstants.NOT_SET) {
      c1 = lookupC1(fromStop, toStop, duration);
    }
    pathBuilder.transfer(toStop, duration, c1);
  }

  /**
   * The duration of the transfer of an arrival. The wait time of a via visit is part of the time
   * between the arrivals, but not of the transfer.
   */
  static int transferDuration(ArrivalView<?> transferArrival) {
    var viaTransfer = transferArrival.viaTransfer();
    if (viaTransfer != null) {
      return viaTransfer.durationInSeconds();
    }
    return transferArrival.hopDuration() - transferArrival.viaWaitTime();
  }

  private int lookupC1(int fromStop, int toStop, int duration) {
    var transfer = transferData.findTransfer(fromStop, toStop, duration);
    if (transfer == null) {
      throw new IllegalStateException(
        "No transfer found from stop " +
          fromStop +
          " to " +
          toStop +
          " with duration <= " +
          duration
      );
    }
    return transfer.c1();
  }
}
