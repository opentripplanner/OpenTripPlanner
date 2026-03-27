package org.opentripplanner.raptor.spi;

import javax.annotation.Nullable;

/**
 * This interface is used by Raptor to create a path from the Raptor state. We do not keep
 * constraints during the search, so we need to look it up when building the path.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorPathConstrainedTransferSearch<T extends RaptorTripSchedule> {
  @Nullable
  RaptorConstrainedTransfer findConstrainedTransfer(
    T fromTrip,
    int fromStopPosition,
    T toTrip,
    int toStopPosition
  );
}
