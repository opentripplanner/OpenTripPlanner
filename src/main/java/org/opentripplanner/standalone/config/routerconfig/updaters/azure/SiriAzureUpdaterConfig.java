package org.opentripplanner.standalone.config.routerconfig.updaters.azure;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public abstract class SiriAzureUpdaterConfig {

  public static void populateConfig(
    SiriAzureUpdaterParameters parameters,
    String configRef,
    NodeAdapter c
  ) {
    parameters.setConfigRef(configRef);
    parameters.setServiceBusUrl(c.of("servicebus-url").since(NA).summary("TODO").asString(null));
    parameters.setTopicName(c.of("topic").since(NA).summary("TODO").asString(null));
    parameters.setFeedId(c.of("feedId").since(NA).summary("TODO").asString(null));
    parameters.setFuzzyTripMatching(
      c.of("fuzzyTripMatching").since(NA).summary("TODO").asBoolean(false)
    );

    if (c.exist("history")) {
      NodeAdapter history = c
        .of("history")
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObject();
      parameters.setDataInitializationUrl(
        history.of("url").since(NA).summary("TODO").asString(null)
      );
      parameters.setTimeout(history.of("timeout").since(NA).summary("TODO").asInt(300000));
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
