package org.opentripplanner.updater.siri.updater;

import org.opentripplanner.updater.siri.updater.lite.SiriETLiteUpdaterParameters;
import org.opentripplanner.updater.siri.updater.lite.SiriLiteHttpLoader;

/**
 * Constructs a SiriLoader from the parameters of the updater.
 */
public class SiriLoaderFactory {

  public static SiriLoader createLoader(SiriETUpdater.Parameters parameters) {
    // Load real-time updates from a file.
    if (SiriFileLoader.matchesUrl(parameters.url())) {
      return new SiriFileLoader(parameters.url());
    }
    // Fallback to default loader
    else {
      return switch (parameters) {
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
        default -> throw new IllegalArgumentException("Unexpected value: " + parameters);
      };
    }
  }
}
