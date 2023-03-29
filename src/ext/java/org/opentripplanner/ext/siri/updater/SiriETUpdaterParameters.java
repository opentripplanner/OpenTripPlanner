package org.opentripplanner.ext.siri.updater;

import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.PollingGraphUpdaterParameters;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;

public record SiriETUpdaterParameters(
  String configRef,
  String feedId,
  boolean blockReadinessUntilInitialized,
  String url,
  int frequencySec,
  String requestorRef,
  int timeoutSec,
  int previewIntervalMinutes,
  boolean fuzzyTripMatching,
  HttpHeaders headers
)
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {
  public SiriETHttpTripUpdateSource.Parameters sourceParameters() {
    return new SiriETHttpTripUpdateSource.Parameters() {
      @Override
      public String getUrl() {
        return url;
      }

      @Override
      public String getRequestorRef() {
        return requestorRef;
      }

      @Override
      public String getFeedId() {
        return feedId;
      }

      @Override
      public int getTimeoutSec() {
        return timeoutSec;
      }

      @Override
      public int getPreviewIntervalMinutes() {
        return previewIntervalMinutes;
      }

      @Override
      public HttpHeaders httpRequestHeaders() {
        return headers;
      }
    };
  }
}
