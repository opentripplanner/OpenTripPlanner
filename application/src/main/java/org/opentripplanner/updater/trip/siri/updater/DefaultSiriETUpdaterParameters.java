package org.opentripplanner.updater.trip.siri.updater;

import java.time.Duration;
import org.opentripplanner.updater.spi.HttpHeaders;

public record DefaultSiriETUpdaterParameters(
  String configRef,
  String feedId,
  boolean blockReadinessUntilInitialized,
  String url,
  Duration frequency,
  String requestorRef,
  Duration timeout,
  Duration previewInterval,
  boolean fuzzyTripMatching,
  HttpHeaders httpRequestHeaders,
  boolean producerMetrics
) implements SiriETUpdaterParameters, SiriETHttpTripUpdateSource.Parameters {}
