package org.opentripplanner.updater.trip.siri.updater;

import java.time.Duration;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatchingParameters;

public record DefaultSiriETUpdaterParameters(
  String configRef,
  String feedId,
  boolean blockReadinessUntilInitialized,
  String url,
  Duration frequency,
  String requestorRef,
  Duration timeout,
  Duration previewInterval,
  SiriFuzzyTripMatchingParameters fuzzyTripMatching,
  HttpHeaders httpRequestHeaders,
  boolean producerMetrics
) implements SiriETUpdaterParameters, SiriETHttpTripUpdateSource.Parameters {}
