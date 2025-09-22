package org.opentripplanner.standalone.config.routerconfig.updaters.azure;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.opentripplanner.ext.siri.updater.azure.AuthenticationType;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public abstract class SiriAzureUpdaterConfig {

  public static void populateConfig(
    SiriAzureUpdaterParameters parameters,
    String configRef,
    NodeAdapter c
  ) {
    parameters.setConfigRef(configRef);
    parameters.setServiceBusUrl(
      c
        .of("servicebus-url")
        .since(V2_2)
        .summary("Service Bus connection used for authentication.")
        .description(
          "Has to be present for authenticationMethod SharedAccessKey. This should be Primary/Secondary connection string from service bus."
        )
        .asString(null)
    );
    parameters.setTopicName(
      c.of("topic").since(V2_2).summary("Service Bus topic to connect to.").asString()
    );
    parameters.setFeedId(
      c.of("feedId").since(V2_2).summary("The ID of the feed to apply the updates to.").asString()
    );
    parameters.setAutoDeleteOnIdle(
      c
        .of("autoDeleteOnIdle")
        .since(V2_5)
        .summary("The time after which an inactive subscription is removed.")
        .asDuration(Duration.ofHours(1))
    );
    parameters.setPrefetchCount(
      c
        .of("prefetchCount")
        .since(V2_5)
        .summary("The number of messages to fetch from the subscription at a time.")
        .asInt(10)
    );
    parameters.setFuzzyTripMatching(
      c
        .of("fuzzyTripMatching")
        .since(V2_2)
        .summary("Whether to apply fuzzyTripMatching on the updates")
        .asBoolean(false)
    );
    parameters.setFullyQualifiedNamespace(
      c
        .of("fullyQualifiedNamespace")
        .since(V2_5)
        .summary("Service Bus fully qualified namespace used for authentication.")
        .description("Has to be present for authenticationMethod FederatedIdentity.")
        .asString(null)
    );
    parameters.setAuthenticationType(
      c
        .of("authenticationType")
        .since(V2_5)
        .summary("Which authentication type to use")
        .asEnum(AuthenticationType.SharedAccessKey)
    );

    if (c.exist("history")) {
      NodeAdapter history = c
        .of("history")
        .since(V2_2)
        .summary("Configuration for fetching historical data on startup")
        .asObject();
      parameters.setDataInitializationUrl(
        history.of("url").since(NA).summary("Endpoint to fetch from").asString(null)
      );
      parameters.setTimeout(
        history.of("timeout").since(NA).summary("Timeout in milliseconds").asInt(300000)
      );
    }

    parameters.setStartupTimeout(
      c
        .of("startupTimeout")
        .summary("Maximum time to wait for real-time services during startup.")
        .description(
          "Maximum time to wait for real-time services during startup. " +
          "If real-time services are unavailable, OTP will start without real-time data after this timeout."
        )
        .asDuration(parameters.getStartupTimeout())
    );
  }

  /**
   * @param text
   * @param customMidnight custom midnight offset
   * @return a relative zonedDateTime
   */
  protected static LocalDate asDateOrRelativePeriod(String text, int customMidnight) {
    try {
      if (text == null || text.isBlank()) {
        return null;
      }
      if (text.startsWith("-") || text.startsWith("P")) {
        return getDateBasedOnCustomMidnight(
          ZonedDateTime.now(ZoneId.of("CET")).plus(Period.parse(text)),
          customMidnight
        );
      } else {
        return LocalDate.parse(text);
      }
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
        text + "' is not a Period or LocalDate. " + "Details: " + e.getLocalizedMessage()
      );
    }
  }

  /**
   * Get current date with offset for a custom midnight, using a zonedDateTime as input.
   *
   * @param time           ZonedDateTime to calculate date from
   * @param customMidnight In order to match the custom day break handling, this value will configure a custom midnight.
   *                       For example: 4 - Then a new day will begin at 4am.
   *                       Int value of a 24 hour clock
   * @return LocalDate
   */
  public static LocalDate getDateBasedOnCustomMidnight(ZonedDateTime time, int customMidnight) {
    if (time.isBefore(time.withHour(customMidnight).withMinute(0).withSecond(0).withNano(0))) {
      return time.minusDays(1).toLocalDate();
    } else {
      return time.toLocalDate();
    }
  }
}
