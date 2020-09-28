package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.DefaultUpdaterDataSourceConfig;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.UpdaterDataSourceConfig;

public class PollingGraphUpdaterConfig
    implements PollingGraphUpdater.PollingGraphUpdaterParameters {

  private final String configRef;

  private final UpdaterDataSourceConfig source;

  private final String url;

  private final int frequencySec;

  public PollingGraphUpdaterConfig(String configRef, NodeAdapter c) {
    this.configRef = configRef;
    this.source = new DefaultUpdaterDataSourceConfig(c);
    this.url = c.asText("url", null);
    this.frequencySec = c.asInt("frequencySec", 60);
  }


  @Override
  public UpdaterDataSourceConfig getSourceConfig() { return source; }

  @Override
  public String getUrl() { return url; }

  @Override
  public int getFrequencySec() { return frequencySec; }

  @Override
  public String getConfigRef() {
    return configRef;
  }
}
