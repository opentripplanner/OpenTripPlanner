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

  public void run() {
    siriMessageSource.start(this::processSiriData);
  }

  public boolean isPrimed() {
    return this.primed;
  }

  private void processSiriData(ServiceDelivery serviceDelivery) {
    var f = estimatedTimetableHandler.applyUpdate(
      serviceDelivery.getEstimatedTimetableDeliveries(),
      false
    );
    if (!isPrimed()) {
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
