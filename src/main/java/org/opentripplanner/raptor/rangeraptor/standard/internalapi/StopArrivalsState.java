package org.opentripplanner.raptor.rangeraptor.standard.internalapi;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.TransitArrival;

/**
 * This interface define a superset of operations to maintain the stop arrivals state, and for the
 * implementation to compute results. The Range Raptor algorithm do NOT depend on the state, only on
 * the {@link BestTimes} - with one exception the {@link #bestTimePreviousRound(int)}.
 * <p/>
 * Different implementations may implement this to:
 * <ul>
 *     <li>Compute paths
 *     <li>Enable debugging
 *     <li>Compute heuristics
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface StopArrivalsState<T extends RaptorTripSchedule> extends BestNumberOfTransfers {
  void setAccessTime(int arrivalTime, RaptorAccessEgress access, boolean bestTime);

  default void rejectAccessTime(int arrivalTime, RaptorAccessEgress access) {}

  int bestTimePreviousRound(int stop);

  void setNewBestTransitTime(
    int alightStop,
    int alightTime,
    T trip,
    int boardStop,
    int boardTime,
    boolean newBestOverall
  );

  default void rejectNewBestTransitTime(
    int alightStop,
    int alightTime,
    T trip,
    int boardStop,
    int boardTime
  ) {}

  void setNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transfer);

  default void rejectNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transfer) {}

  @Nullable
  TransitArrival<T> previousTransit(int boardStopIndex);

  default Collection<Path<T>> extractPaths() {
    return List.of();
  }
}
