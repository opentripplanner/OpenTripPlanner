package org.opentripplanner.ext.siri.updater;

import java.util.concurrent.ExecutionException;
import uk.org.siri.siri20.ServiceDelivery;

/**
 * Process a SIRI-ET feed by combining an asynchronous source of estimated timetables
 * {@link AsyncEstimatedTimetableSource} with a consumer of estimated timetables
 * {@link EstimatedTimetableHandler}
 */
public class AsyncEstimatedTimetableProcessor {

  private final AsyncEstimatedTimetableSource siriMessageSource;
  private final EstimatedTimetableHandler estimatedTimetableHandler;

  private volatile boolean primed;

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
   * Return true if the estimated timetable source is initialized and the backlog of messages
   * is processed.
   */
  public boolean isPrimed() {
    return primed;
  }

  /**
   * Apply the estimated timetables to the transit model.
   * The first successful call to this method sets the primed status to true.
   */
  private void processSiriData(ServiceDelivery serviceDelivery) {
    var f = estimatedTimetableHandler.applyUpdate(
      serviceDelivery.getEstimatedTimetableDeliveries(),
      false
    );
    if (!primed) {
      try {
        f.get();
        primed = true;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
