package org.opentripplanner.standalone.config.updaters.azure;

import org.opentripplanner.ext.siri.updater.azure.SiriAzureSXUpdaterParameters;
import org.opentripplanner.standalone.config.NodeAdapter;

public class SiriAzureSXUpdaterConfig extends SiriAzureUpdaterConfig {

  public static SiriAzureSXUpdaterParameters create(String configRef, NodeAdapter c) {
    SiriAzureSXUpdaterParameters parameters = new SiriAzureSXUpdaterParameters();
    populateConfig(parameters, configRef, c);

    if (c.exist("history")) {
      NodeAdapter history = c.path("history");

      String fromDateTime = history.asText("fromDateTime", "-P1D");
      String toDateTime = history.asText("toDateTime", "P1D");
      int customMidnight = c.asInt("customMidnight", 0);

      parameters.setFromDateTime(asDateOrRelativePeriod(fromDateTime, customMidnight));
      parameters.setToDateTime(asDateOrRelativePeriod(toDateTime, customMidnight));
    }

    return parameters;
  }
}
