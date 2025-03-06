package org.opentripplanner.updater.trip.siri.updater;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import uk.org.siri.siri20.ServiceDelivery;

/**
 * Apply asynchronously estimated timetable updates in the graph-writer thread and forward the
 * result to an update result consumer.
 */
public class AsyncEstimatedTimetableProcessor {

  private final EstimatedTimetableHandler estimatedTimetableHandler;
  private final WriteToGraphCallback saveResultOnGraph;
  private final Consumer<UpdateResult> updateResultConsumer;

  public AsyncEstimatedTimetableProcessor(
    EstimatedTimetableHandler estimatedTimetableHandler,
    WriteToGraphCallback saveResultOnGraph,
    Consumer<UpdateResult> updateResultConsumer
  ) {
    this.estimatedTimetableHandler = estimatedTimetableHandler;
    this.saveResultOnGraph = saveResultOnGraph;
    this.updateResultConsumer = updateResultConsumer;
  }

  /**
   * Apply the estimated timetables to the transit model.
   * This method is non-blocking and applies the changes asynchronously.
   * @return a future indicating when the changes are applied.
   */
  public Future<?> processSiriData(ServiceDelivery serviceDelivery) {
    return saveResultOnGraph.execute(context ->
      updateResultConsumer.accept(
        estimatedTimetableHandler.applyUpdate(
          serviceDelivery.getEstimatedTimetableDeliveries(),
          UpdateIncrementality.DIFFERENTIAL,
          context
        )
      )
    );
  }
}
