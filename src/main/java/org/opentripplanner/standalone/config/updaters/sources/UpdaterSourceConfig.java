package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.UpdaterDataSourceParameters;

public class UpdaterSourceConfig implements UpdaterDataSourceParameters {
  private final DataSourceType type;
  private final String url;

  public UpdaterSourceConfig(DataSourceType type, NodeAdapter c) {
    this.type = type;
    url = c.asText("url", null);
  }

  @Override
  public DataSourceType type() {
    return type;
  }

  @Override
  public String getUrl() { return url; }

}
