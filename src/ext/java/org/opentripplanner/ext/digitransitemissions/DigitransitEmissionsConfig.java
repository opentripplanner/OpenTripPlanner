package org.opentripplanner.ext.digitransitemissions;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping Digitransit emissions configuration into Digitransit emissions parameters.
 */
public class DigitransitEmissionsConfig {

  private String url;

  public DigitransitEmissionsConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_4)
      .summary("Configure properties for emissions file.")
      .description(
        """
        By specifying a URL to fetch emissions data, the program gains access to carbon dioxide (CO2)
        emissions associated with transportation modes. This data is then used
        to perform emission calculations for public transport modes and car travel.
        """
      )
      .asObject();

    this.url = c.of("url").since(V2_4).summary("Url to emissions json file.").asString("");
  }

  public String getUrl() {
    return url;
  }
}
