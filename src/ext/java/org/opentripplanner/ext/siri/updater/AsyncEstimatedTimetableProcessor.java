package org.opentripplanner.ext.siri.updater;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
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
  private final WriteToGraphCallback saveResultOnGraph;
  private final Consumer<UpdateResult> updateResultConsumer;

  public AsyncEstimatedTimetableProcessor(
    AsyncEstimatedTimetableSource siriMessageSource,
    EstimatedTimetableHandler estimatedTimetableHandler,
    WriteToGraphCallback saveResultOnGraph,
    Consumer<UpdateResult> updateResultConsumer
  ) {
    this.siriMessageSource = siriMessageSource;
    this.estimatedTimetableHandler = estimatedTimetableHandler;
    this.saveResultOnGraph = saveResultOnGraph;
    this.updateResultConsumer = updateResultConsumer;
  }

  /**
   * Start consuming from the estimated timetable source.
   */
  public void run() {
    siriMessageSource.start(this::processSiriData);
  }

  /**
   * Apply the estimated timetables to the transit model.
   * This method is non-blocking and applies the changes asynchronously.
   * @return a future indicating when the changes are applied.
   */
  private Future<?> processSiriData(ServiceDelivery serviceDelivery) {
    return saveResultOnGraph.execute((graph, transitModel) ->
      updateResultConsumer.accept(
        estimatedTimetableHandler.applyUpdate(
          serviceDelivery.getEstimatedTimetableDeliveries(),
          UpdateIncrementality.DIFFERENTIAL
        )
      )
    );
  }
}
