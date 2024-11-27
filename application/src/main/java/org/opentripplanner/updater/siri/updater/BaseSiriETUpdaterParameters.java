package org.opentripplanner.updater.siri.updater;

import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

public interface BaseSiriETUpdaterParameters
  extends UrlUpdaterParameters, PollingGraphUpdaterParameters {
  boolean blockReadinessUntilInitialized();

  boolean fuzzyTripMatching();
}
