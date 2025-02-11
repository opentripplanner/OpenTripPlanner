package org.opentripplanner.updater.siri.updater.configure;

import java.util.function.Consumer;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.siri.updater.EstimatedTimetableSource;
import org.opentripplanner.updater.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.updater.siri.updater.SiriETUpdater;
import org.opentripplanner.updater.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.updater.siri.updater.SiriFileLoader;
import org.opentripplanner.updater.siri.updater.SiriHttpLoader;
import org.opentripplanner.updater.siri.updater.SiriLoader;
import org.opentripplanner.updater.siri.updater.SiriSXUpdater;
import org.opentripplanner.updater.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.updater.siri.updater.lite.SiriETLiteHttpTripUpdateSource;
import org.opentripplanner.updater.siri.updater.lite.SiriETLiteUpdaterParameters;
import org.opentripplanner.updater.siri.updater.lite.SiriLiteHttpLoader;
import org.opentripplanner.updater.siri.updater.lite.SiriSXLiteUpdaterParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;

/**
 * Dependency injection for instantiating SIRI-ET and SX updaters.
 */
public class SiriUpdaterModule {

  public static SiriETUpdater createSiriETUpdater(
    SiriETUpdater.Parameters params,
    SiriRealTimeTripUpdateAdapter adapter
  ) {
    return new SiriETUpdater(params, adapter, createSource(params), createMetricsConsumer(params));
  }

  public static SiriSXUpdater createSiriSXUpdater(
    SiriSXUpdater.Parameters params,
    TimetableRepository timetableRepository
  ) {
    return new SiriSXUpdater(params, timetableRepository, createLoader(params));
  }

  private static EstimatedTimetableSource createSource(SiriETUpdater.Parameters params) {
    return switch (params) {
      case SiriETUpdaterParameters p -> new SiriETHttpTripUpdateSource(
        p.sourceParameters(),
        createLoader(params)
      );
      case SiriETLiteUpdaterParameters p -> new SiriETLiteHttpTripUpdateSource(
        p.sourceParameters(),
        createLoader(params)
      );
      default -> throw new IllegalArgumentException("Unexpected value: " + params);
    };
  }

  private static SiriLoader createLoader(SiriSXUpdater.Parameters params) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(params.url())) {
      return new SiriFileLoader(params.url());
    }
    // Fallback to default loader
    return switch (params) {
      case SiriSXUpdaterParameters p -> new SiriHttpLoader(
        p.url(),
        p.timeout(),
        p.requestHeaders()
      );
      case SiriSXLiteUpdaterParameters p -> new SiriLiteHttpLoader(
        p.uri(),
        p.timeout(),
        p.requestHeaders()
      );
      default -> throw new IllegalArgumentException("Unexpected value: " + params);
    };
  }

  private static SiriLoader createLoader(SiriETUpdater.Parameters params) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(params.url())) {
      return new SiriFileLoader(params.url());
    }
    // Fallback to default loader
    else {
      return switch (params) {
        case SiriETUpdaterParameters p -> new SiriHttpLoader(
          p.url(),
          p.timeout(),
          p.httpRequestHeaders(),
          p.previewInterval()
        );
        case SiriETLiteUpdaterParameters p -> new SiriLiteHttpLoader(
          p.uri(),
          p.timeout(),
          p.httpRequestHeaders()
        );
        default -> throw new IllegalArgumentException("Unexpected value: " + params);
      };
    }
  }

  private static Consumer<UpdateResult> createMetricsConsumer(SiriETUpdater.Parameters params) {
    return switch (params) {
      case SiriETUpdaterParameters p -> TripUpdateMetrics.streaming(p);
      case SiriETLiteUpdaterParameters p -> TripUpdateMetrics.batch(p);
      default -> throw new IllegalArgumentException("Unexpected value: " + params);
    };
  }
}
