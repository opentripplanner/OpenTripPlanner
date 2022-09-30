package org.opentripplanner.ext.siri.updater;

import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.util.lang.ToStringBuilder;

public record SiriETGooglePubsubUpdaterParameters(
  @Nonnull String configRef,
  @Nullable String feedId,
  String type,
  String projectName,
  String topicName,
  @Nullable String dataInitializationUrl,
  Duration reconnectPeriod,
  Duration initialGetDataTimeout,
  boolean purgeExpiredData
) {
  public static Duration RECONNECT_PERIOD = Duration.ofSeconds(30);
  public static Duration INITIAL_GET_DATA_TIMEOUT = Duration.ofSeconds(30);

  public SiriETGooglePubsubUpdaterParameters {
    Objects.requireNonNull(type);
    Objects.requireNonNull(projectName);
    Objects.requireNonNull(topicName);
    Objects.requireNonNull(reconnectPeriod);
    Objects.requireNonNull(initialGetDataTimeout);
    Objects.requireNonNull(reconnectPeriod);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(SiriETGooglePubsubUpdaterParameters.class)
      .addObj("configRef", configRef, null)
      .addObj("feedId", feedId, null)
      .addObj("type", type)
      .addObj("projectName", projectName)
      .addObj("topicName", topicName)
      .addDuration("reconnectPeriod", reconnectPeriod, RECONNECT_PERIOD)
      .addDuration("initialGetDataTimeout", initialGetDataTimeout, INITIAL_GET_DATA_TIMEOUT)
      .addBoolIfTrue("purgeExpiredData", purgeExpiredData)
      .addObj("dataInitializationUrl", dataInitializationUrl, null)
      .toString();
  }
}
