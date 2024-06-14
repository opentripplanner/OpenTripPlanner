package org.opentripplanner.ext.siri.updater;

import java.util.concurrent.Future;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import uk.org.siri.siri20.ServiceDelivery;

/**
 * Process a SIRI-ET feed by combining an asynchronous source of estimated timetables
 * {@link AsyncEstimatedTimetableSource} with a consumer of estimated timetables
 * {@link EstimatedTimetableHandler}
 */
public class AsyncEstimatedTimetableProcessor {

  private final AsyncEstimatedTimetableSource siriMessageSource;
  private final EstimatedTimetableHandler estimatedTimetableHandler;

  public AsyncEstimatedTimetableProcessor(
    AsyncEstimatedTimetableSource siriMessageSource,
    EstimatedTimetableHandler estimatedTimetableHandler
  ) {
    this.siriMessageSource = siriMessageSource;
    this.estimatedTimetableHandler = estimatedTimetableHandler;
  }

  /**
   * Start consuming from the estimated timetable source.
   */
  public void run() {
    siriMessageSource.start(this::processSiriData);
  }

  /**
   * Apply the estimated timetables to the transit model.
   * This method is non-blocking and applies the changes asynchronosly.
   * @return a future indicating when the changes are applied.
   */
  private Future<?> processSiriData(ServiceDelivery serviceDelivery) {
    return estimatedTimetableHandler.applyUpdate(
      serviceDelivery.getEstimatedTimetableDeliveries(),
      UpdateIncrementality.DIFFERENTIAL
    );
  }
}
