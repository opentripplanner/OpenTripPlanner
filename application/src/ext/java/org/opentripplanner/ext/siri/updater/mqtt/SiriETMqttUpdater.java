package org.opentripplanner.ext.siri.updater.mqtt;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.siri.updater.AsyncEstimatedTimetableProcessor;
import org.opentripplanner.updater.trip.siri.updater.AsyncEstimatedTimetableSource;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableHandler;

public class SiriETMqttUpdater implements GraphUpdater {

  private final String configRef;

  private final AsyncEstimatedTimetableSource asyncEstimatedTimetableSource;
  private final EstimatedTimetableHandler estimatedTimetableHandler;
  private final Consumer<UpdateResult> updateResultConsumer;
  private WriteToGraphCallback writeToGraphCallback;

  public SiriETMqttUpdater(
    MqttSiriETUpdaterParameters parameters,
    SiriRealTimeTripUpdateAdapter updateAdapter
  ) {
    configRef = parameters.configRef();
    asyncEstimatedTimetableSource = new MqttEstimatedTimetableSource(parameters);
    estimatedTimetableHandler = new EstimatedTimetableHandler(updateAdapter, parameters.feedId());
    updateResultConsumer = TripUpdateMetrics.streaming(parameters);
  }

  @Override
  public void setup(@Nonnull WriteToGraphCallback writeToGraphCallback) {
    this.writeToGraphCallback = writeToGraphCallback;
  }

  @Override
  public void run() throws Exception {
    AsyncEstimatedTimetableProcessor asyncEstimatedTimetableProcessor =
      new AsyncEstimatedTimetableProcessor(
        estimatedTimetableHandler,
        writeToGraphCallback,
        updateResultConsumer
      );
    asyncEstimatedTimetableSource.start(asyncEstimatedTimetableProcessor::processSiriData);
  }

  @Override
  public void teardown() {
    asyncEstimatedTimetableSource.teardown();
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }

  @Override
  public boolean isPrimed() {
    return asyncEstimatedTimetableSource.isPrimed();
  }
}
