package org.opentripplanner.ext.siri.updater;

import java.util.concurrent.Future;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphWriterRunnable;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.WriteToGraphCallback;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;

/**
 * Graph updater that processes a SIRI-ET feed based on a Google Pubsub subscription. This class
 * configures a {@link GooglePubsubEstimatedTimetableSource} and an {@link EstimatedTimetableHandler}
 * and delegates the update process to {@link AsyncEstimatedTimetableProcessor}
 */
public class SiriETGooglePubsubUpdater implements GraphUpdater {

  private final String configRef;
  private final AsyncEstimatedTimetableProcessor asyncEstimatedTimetableProcessor;
  private final AsyncEstimatedTimetableSource asyncSiriMessageSource;
  private WriteToGraphCallback saveResultOnGraph;

  public SiriETGooglePubsubUpdater(
    SiriETGooglePubsubUpdaterParameters config,
    TransitModel transitModel,
    SiriTimetableSnapshotSource timetableSnapshot
  ) {
    this.configRef = config.configRef();

    asyncSiriMessageSource =
      new GooglePubsubEstimatedTimetableSource(
        config.dataInitializationUrl(),
        config.reconnectPeriod(),
        config.initialGetDataTimeout(),
        config.subscriptionProjectName(),
        config.topicProjectName(),
        config.topicName()
      );

    EstimatedTimetableHandler estimatedTimetableHandler = new EstimatedTimetableHandler(
      this::writeToGraphCallBack,
      timetableSnapshot,
      config.fuzzyTripMatching(),
      new DefaultTransitService(transitModel),
      TripUpdateMetrics.streaming(config),
      config.feedId()
    );

    this.asyncEstimatedTimetableProcessor =
      new AsyncEstimatedTimetableProcessor(asyncSiriMessageSource, estimatedTimetableHandler);
  }

  private Future<?> writeToGraphCallBack(GraphWriterRunnable graphWriterRunnable) {
    return saveResultOnGraph.execute(graphWriterRunnable);
  }

  @Override
  public void setup(WriteToGraphCallback writeToGraphCallback) {
    this.saveResultOnGraph = writeToGraphCallback;
  }

  @Override
  public void run() {
    asyncEstimatedTimetableProcessor.run();
  }

  @Override
  public boolean isPrimed() {
    return asyncSiriMessageSource.isPrimed();
  }

  @Override
  public String getConfigRef() {
    return configRef;
  }
}
