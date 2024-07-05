package org.opentripplanner.standalone.config.routerconfig.updaters.azure;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.ext.siri.updater.azure.SiriAzureETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriAzureETUpdaterConfig extends SiriAzureUpdaterConfig {

  public static SiriAzureETUpdaterParameters create(String configRef, NodeAdapter c) {
    SiriAzureETUpdaterParameters parameters = new SiriAzureETUpdaterParameters();
    populateConfig(parameters, configRef, c);

    if (c.exist("history")) {
      NodeAdapter history = c
        .of("history")
        .since(V2_2)
        .summary("Configuration for fetching historical data on startup")
        .asObject();

      String fromDateTime = history
        .of("fromDateTime")
        .since(V2_2)
        .summary("Datetime boundary for historical data")
        .asString("-P1D");

      int customMidnight = c
        .of("customMidnight")
        .since(V2_2)
        .summary("Time on which time breaks into new day.")
        .description(
          "It is common that operating day date breaks a little bit later than midnight so " +
          "that the switch happens when traffic is at the lowest point. Parameter uses 24-hour format. " +
          "If the switch happens on 4 am then set this field to 4."
        )
        .asInt(0);

      parameters.setFromDateTime(asDateOrRelativePeriod(fromDateTime, customMidnight));
    }

    return parameters;
  }
}
