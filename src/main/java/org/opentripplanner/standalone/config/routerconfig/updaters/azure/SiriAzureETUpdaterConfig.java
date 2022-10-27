package org.opentripplanner.standalone.config.routerconfig.updaters.azure;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.ext.siri.updater.azure.SiriAzureETUpdaterParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class SiriAzureETUpdaterConfig extends SiriAzureUpdaterConfig {

  public static SiriAzureETUpdaterParameters create(String configRef, NodeAdapter c) {
    SiriAzureETUpdaterParameters parameters = new SiriAzureETUpdaterParameters();
    populateConfig(parameters, configRef, c);

    if (c.exist("history")) {
      NodeAdapter history = c
        .of("history")
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObject();

      String fromDateTime = history.of("fromDateTime").since(NA).summary("TODO").asString("-P1D");
      int customMidnight = c.of("customMidnight").since(NA).summary("TODO").asInt(0);

      parameters.setFromDateTime(asDateOrRelativePeriod(fromDateTime, customMidnight));
    }

    return parameters;
  }
}
