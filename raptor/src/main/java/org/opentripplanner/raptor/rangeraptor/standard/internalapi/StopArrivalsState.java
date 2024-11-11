package org.opentripplanner.raptor.rangeraptor.standard.internalapi;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.TransitArrival;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimes;

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
public interface StopArrivalsState<T extends RaptorTripSchedule> {
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

  Collection<RaptorPath<T>> extractPaths();
}
