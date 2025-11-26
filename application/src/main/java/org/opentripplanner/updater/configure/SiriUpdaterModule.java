package org.opentripplanner.updater.configure;

import java.util.function.Consumer;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.alert.siri.SiriSXUpdater;
import org.opentripplanner.updater.alert.siri.SiriSXUpdaterParameters;
import org.opentripplanner.updater.alert.siri.lite.SiriLiteHttpLoader;
import org.opentripplanner.updater.alert.siri.lite.SiriSXLiteUpdaterParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.support.siri.SiriFileLoader;
import org.opentripplanner.updater.support.siri.SiriHttpLoader;
import org.opentripplanner.updater.support.siri.SiriLoader;
import org.opentripplanner.updater.trip.metrics.TripUpdateMetrics;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.siri.updater.DefaultSiriETUpdaterParameters;
import org.opentripplanner.updater.trip.siri.updater.EstimatedTimetableSource;
import org.opentripplanner.updater.trip.siri.updater.SiriETHttpTripUpdateSource;
import org.opentripplanner.updater.trip.siri.updater.SiriETUpdater;
import org.opentripplanner.updater.trip.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.updater.trip.siri.updater.lite.SiriETLiteHttpTripUpdateSource;
import org.opentripplanner.updater.trip.siri.updater.lite.SiriETLiteUpdaterParameters;

/**
 * Dependency injection for instantiating SIRI-ET and SX updaters.
 */
public class SiriUpdaterModule {

  public static SiriETUpdater createSiriETUpdater(
    SiriETUpdaterParameters params,
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

  private static EstimatedTimetableSource createSource(SiriETUpdaterParameters params) {
    return switch (params) {
      case DefaultSiriETUpdaterParameters p -> new SiriETHttpTripUpdateSource(
        p,
        createLoader(params)
      );
      case SiriETLiteUpdaterParameters p -> new SiriETLiteHttpTripUpdateSource(
        p,
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

  private static SiriLoader createLoader(SiriETUpdaterParameters params) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(params.url())) {
      return new SiriFileLoader(params.url());
    }
    // Fallback to default loader
    else {
      return switch (params) {
        case DefaultSiriETUpdaterParameters p -> new SiriHttpLoader(
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

  private static Consumer<UpdateResult> createMetricsConsumer(SiriETUpdaterParameters params) {
    return switch (params) {
      case DefaultSiriETUpdaterParameters p -> TripUpdateMetrics.streaming(p);
      case SiriETLiteUpdaterParameters p -> TripUpdateMetrics.batch(p);
      default -> throw new IllegalArgumentException("Unexpected value: " + params);
    };
  }
}
