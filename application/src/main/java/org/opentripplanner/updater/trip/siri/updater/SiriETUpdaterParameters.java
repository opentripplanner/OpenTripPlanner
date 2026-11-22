package org.opentripplanner.updater.trip.siri.updater;

import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatchingParameters;

public interface SiriETUpdaterParameters
  extends UrlUpdaterParameters, PollingGraphUpdaterParameters
{
  String url();

  boolean blockReadinessUntilInitialized();

  /** The fuzzy trip matching configuration of this updater. */
  SiriFuzzyTripMatchingParameters fuzzyTripMatching();
}
