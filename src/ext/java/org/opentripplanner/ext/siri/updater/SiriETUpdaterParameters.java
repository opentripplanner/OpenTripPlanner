package org.opentripplanner.ext.siri.updater;

import java.util.Map;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;
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
  Map<String, String> headers
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
      public Map<String, String> headers() {
        return headers;
      }
    };
  }
}
