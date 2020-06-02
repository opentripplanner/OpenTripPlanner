package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.UpdaterDataSourceParameters;

public class UpdaterSourceParameters implements UpdaterDataSourceParameters {
  private final String url;

  public UpdaterSourceParameters(NodeAdapter c) {
    url = c.asText("url", null);
  }

  public String getUrl() { return url; }

  public String getName() { return url; }
}
