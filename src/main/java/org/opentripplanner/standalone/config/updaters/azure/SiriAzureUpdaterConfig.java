package org.opentripplanner.standalone.config.updaters.azure;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

public abstract class SiriAzureUpdaterConfig {

  public static void populateConfig(
    SiriAzureUpdaterParameters parameters,
    String configRef,
    NodeAdapter c
  ) {
    parameters.setConfigRef(configRef);
    parameters.setServiceBusUrl(c.asText("servicebus-url", null));
    parameters.setTopicName(c.asText("topic", null));
    parameters.setFeedId(c.asText("feedId", null));

    if (c.exist("history")) {
      NodeAdapter history = c.path("history");
      parameters.setDataInitializationUrl(history.asText("url", null));
      parameters.setTimeout(history.asInt("timeout", 300000));
    }
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
