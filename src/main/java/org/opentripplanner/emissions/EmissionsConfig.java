package org.opentripplanner.emissions;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_4;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping OSM configuration into OSM parameters.
 */
public class EmissionsConfig {

  private String url;
  private String configName;

  public EmissionsConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_4)
      .summary("Configure properties for emissions file.")
      .description(
        """
        By specifying a URL to fetch emissions data, the program gains access to carbon dioxide (CO2)
        emissions associated with various transportation modes and routes. This data is then used
        for perform emission calculations to different transport modes and car.
        """
      )
      .asObject();

    this.url = c.of("url").since(V2_4).summary("Url to emissions json file.").asString("");
    this.configName = parameterName;
  }

  public String getUrl() {
    return url;
  }

  public String getConfigName() {
    return configName;
  }
}
