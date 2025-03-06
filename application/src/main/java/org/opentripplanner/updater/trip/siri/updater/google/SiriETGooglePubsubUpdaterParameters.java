package org.opentripplanner.updater.trip.siri.updater.google;

import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.updater.trip.UrlUpdaterParameters;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public record SiriETGooglePubsubUpdaterParameters(
  String configRef,
  @Nullable String feedId,
  String subscriptionProjectName,
  String topicProjectName,
  String topicName,
  @Nullable String dataInitializationUrl,
  Duration reconnectPeriod,
  Duration initialGetDataTimeout,
  boolean fuzzyTripMatching,
  boolean producerMetrics
)
  implements UrlUpdaterParameters {
  public static Duration RECONNECT_PERIOD = Duration.ofSeconds(30);
  public static Duration INITIAL_GET_DATA_TIMEOUT = Duration.ofSeconds(30);

  public SiriETGooglePubsubUpdaterParameters {
    Objects.requireNonNull(subscriptionProjectName);
    Objects.requireNonNull(topicProjectName);
    Objects.requireNonNull(topicName);
    Objects.requireNonNull(reconnectPeriod);
    Objects.requireNonNull(initialGetDataTimeout);
    Objects.requireNonNull(reconnectPeriod);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(SiriETGooglePubsubUpdaterParameters.class)
      .addObj("configRef", configRef, null)
      .addObj("feedId", feedId, null)
      .addObj("subscriptionProjectName", subscriptionProjectName)
      .addObj("topicProjectName", topicProjectName)
      .addObj("topicName", topicName)
      .addDuration("reconnectPeriod", reconnectPeriod, RECONNECT_PERIOD)
      .addDuration("initialGetDataTimeout", initialGetDataTimeout, INITIAL_GET_DATA_TIMEOUT)
      .addBoolIfTrue("fuzzyTripMatching", fuzzyTripMatching)
      .addObj("dataInitializationUrl", dataInitializationUrl, null)
      .toString();
  }

  @Override
  public String url() {
    return dataInitializationUrl;
  }
}
