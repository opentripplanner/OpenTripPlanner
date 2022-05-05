package org.opentripplanner.standalone.config.updaters.azure;

import org.opentripplanner.ext.siri.updater.azure.SiriAzureETUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

public class SiriAzureETUpdaterConfig extends SiriAzureUpdaterConfig {

  public static SiriAzureETUpdaterParameters create(String configRef, NodeAdapter c) {
    SiriAzureETUpdaterParameters parameters = new SiriAzureETUpdaterParameters();
    populateConfig(parameters, configRef, c);

    if (c.exist("history")) {
      NodeAdapter history = c.path("history");

      String fromDateTime = history.asText("fromDateTime", "-P1D");
      int customMidnight = c.asInt("customMidnight", 0);

      parameters.setFromDateTime(asDateOrRelativePeriod(fromDateTime, customMidnight));
    }

    return parameters;
  }
}
