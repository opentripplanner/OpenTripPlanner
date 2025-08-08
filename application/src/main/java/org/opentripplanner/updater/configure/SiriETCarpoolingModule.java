package org.opentripplanner.updater.configure;

import java.util.function.Consumer;
import org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdater;
import org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdaterParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.support.siri.SiriFileLoader;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableSource;

public class SiriETCarpoolingModule {

  public static SiriETCarpoolingUpdater createSiriETCarpoolingUpdater(
    SiriETCarpoolingUpdaterParameters params,
    SiriRealTimeTripUpdateAdapter adapter
  ) {
    return null;
    //return new SiriETCarpoolingUpdater(params, adapter, createSource(params), createMetricsConsumer(params));
  }

  private static EstimatedTimetableSource createSource(SiriETCarpoolingUpdater.Parameters params) {
    return null;
    /**
    return new new SiriETHttpTripUpdateSource(
      params.sourceParameters(),
      createLoader(params)
    );
    */
  }

  private static SiriLoader createLoader(SiriETCarpoolingUpdater.Parameters params) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(params.url())) {
      return new SiriFileLoader(params.url());
    }
    return null;
    /**
    return new SiriHttpLoader(
      params.url(),
      params.timeout(),
      params.httpRequestHeaders(),
      params.previewInterval()
    );
     */
  }

  private static Consumer<UpdateResult> createMetricsConsumer(
    SiriETCarpoolingUpdater.Parameters params
  ) {
    return TripUpdateMetrics.streaming(params);
  }
}
