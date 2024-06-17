package org.opentripplanner.ext.siri.updater.google;

import java.util.concurrent.Future;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.ext.siri.updater.AsyncEstimatedTimetableProcessor;
import org.opentripplanner.ext.siri.updater.AsyncEstimatedTimetableSource;
import org.opentripplanner.ext.siri.updater.EstimatedTimetableHandler;
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
    SiriTimetableSnapshotSource timetableSnapshotSource
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
      timetableSnapshotSource,
      config.fuzzyTripMatching(),
      new DefaultTransitService(transitModel),
      config.feedId()
    );

    asyncEstimatedTimetableProcessor =
      new AsyncEstimatedTimetableProcessor(
        asyncSiriMessageSource,
        estimatedTimetableHandler,
        this::writeToGraphCallBack,
        TripUpdateMetrics.streaming(config)
      );
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

  /**
   * Defer the invocation of {@link #saveResultOnGraph} since it is null at construction time and
   * initialized when {@link #setup(WriteToGraphCallback)} is called.
   */
  private Future<?> writeToGraphCallBack(GraphWriterRunnable graphWriterRunnable) {
    return saveResultOnGraph.execute(graphWriterRunnable);
  }
}
