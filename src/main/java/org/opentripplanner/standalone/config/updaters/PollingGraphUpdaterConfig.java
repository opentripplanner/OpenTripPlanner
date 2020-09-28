package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.DefaultUpdaterDataSourceConfig;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.UpdaterDataSourceConfig;

public class PollingGraphUpdaterConfig
    implements PollingGraphUpdater.PollingGraphUpdaterParameters {

  private final UpdaterDataSourceConfig source;

  private final String url;

  private final int frequencySec;

  public PollingGraphUpdaterConfig(NodeAdapter c) {
    source = new DefaultUpdaterDataSourceConfig(c);
    url = c.asText("url", null);
    frequencySec = c.asInt("frequencySec", 60);
  }


  public UpdaterDataSourceConfig getSourceConfig() { return source; }

  public String getUrl() { return url; }

  public int getFrequencySec() { return frequencySec; }
}
