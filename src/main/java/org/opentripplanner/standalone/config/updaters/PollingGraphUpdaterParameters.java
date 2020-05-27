package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.UpdaterDataSourceConfig;
import org.opentripplanner.updater.PollingGraphUpdater;

public class PollingGraphUpdaterParameters
    implements PollingGraphUpdater.PollingGraphUpdaterParameters {

  private final UpdaterDataSourceConfig source;

  private final String url;

  private final int frequencySec;

  public PollingGraphUpdaterParameters(NodeAdapter c) {
    source = new org.opentripplanner.standalone.config.UpdaterDataSourceConfig(c);
    url = c.asText("url", null);
    frequencySec = c.asInt("frequencySec", 60);
  }


  public UpdaterDataSourceConfig getSourceConfig() { return source; }

  public String getUrl() { return url; }

  public int getFrequencySec() { return frequencySec; }
}
