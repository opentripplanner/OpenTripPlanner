package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.function.IntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This a just an adaptor to look up transfers constraints. The adaptor hides the {@link
 * TransferService} specific API and functions as a bridge to the {@code transferoptimization}
 * model. The best solution would be to use the same mechanism in Raptor and here, but that would
 * require the main transit model to be refactored.
 * <p>
 * The adaptor makes it easy to test the {@link TransferGenerator} by mocking.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class TransferServiceAdaptor<T extends RaptorTripSchedule> {

  private final IntFunction<StopLocation> stopLookup;
  private final TransferService transferService;

  protected TransferServiceAdaptor(
    IntFunction<StopLocation> stopLookup,
    TransferService transferService
  ) {
    this.stopLookup = stopLookup;
    this.transferService = transferService;
  }

  public static <T extends RaptorTripSchedule> TransferServiceAdaptor<T> create(
    IntFunction<StopLocation> stopLookup,
    TransferService transferService
  ) {
    return new TransferServiceAdaptor<>(stopLookup, transferService);
  }

  public static <T extends RaptorTripSchedule> TransferServiceAdaptor<T> noop() {
    return new TransferServiceAdaptor<>(null, null) {
      @Override
      protected ConstrainedTransfer findTransfer(
        TripStopTime<T> from,
        T toTrip,
        int toStop,
        int toStopPosition
      ) {
        return null;
      }
    };
  }

  /**
   * @param toStopPosition First possible stop position in target trip. This is needed because trip pattern
   *                         may visit same stop more than once.
   */
  @Nullable
  protected ConstrainedTransfer findTransfer(
    TripStopTime<T> from,
    T toTrip,
    int toStop,
    int toStopPosition
  ) {
    return transferService.findTransfer(
      trip(from.trip()),
      from.stopPosition(),
      stop(from.stop()),
      trip(toTrip),
      toStopPosition,
      stop(toStop)
    );
  }

  private StopLocation stop(int index) {
    return stopLookup.apply(index);
  }

  private Trip trip(T raptorTripSchedule) {
    return ((TripSchedule) raptorTripSchedule).getOriginalTripTimes().getTrip();
  }
}
