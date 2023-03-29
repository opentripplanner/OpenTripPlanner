package org.opentripplanner.updater.trip;

import java.util.Map;
import org.opentripplanner.updater.PollingGraphUpdaterParameters;

public record PollingTripUpdaterParameters(
  String configRef,
  int frequencySec,
  boolean fuzzyTripMatching,
  BackwardsDelayPropagationType backwardsDelayPropagationType,

  String feedId,
  String httpSourceUrl,
  String fileSource,
  Map<String, String> headers
)
  implements PollingGraphUpdaterParameters, UrlUpdaterParameters {
  public PollingTripUpdaterParameters {
    headers = Map.copyOf(headers);
  }

  @Override
  public String url() {
    return httpSourceUrl;
  }

  @Override
  public String feedId() {
    return feedId;
  }

  BackwardsDelayPropagationType getBackwardsDelayPropagationType() {
    return backwardsDelayPropagationType;
  }

  GtfsRealtimeFileTripUpdateSource.Parameters fileSourceParameters() {
    return new GtfsRealtimeFileTripUpdateSource.Parameters() {
      @Override
      public String getFeedId() {
        return feedId;
      }

      @Override
      public String getFile() {
        return fileSource;
      }
    };
  }

  GtfsRealtimeHttpTripUpdateSource.Parameters httpSourceParameters() {
    return new GtfsRealtimeHttpTripUpdateSource.Parameters() {
      @Override
      public String getFeedId() {
        return feedId;
      }

      @Override
      public String getUrl() {
        return httpSourceUrl;
      }

      @Override
      public Map<String, String> headers() {
        return headers;
      }
    };
  }
}
