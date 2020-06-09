package org.opentripplanner.standalone.config.updaters;

import org.apache.commons.lang3.StringUtils;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.DefaultUpdaterDataSourceConfig;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.UpdaterDataSourceConfig;

public class PollingGraphUpdaterParameters
    implements PollingGraphUpdater.PollingGraphUpdaterParameters {

  private final UpdaterDataSourceConfig source;

  private final String url;

  private final int frequencySec;

  public PollingGraphUpdaterParameters(NodeAdapter c) {
    if(c.exist("sourceType")){
      source = new DefaultUpdaterDataSourceConfig(c);
    }else {
      source = null;
    }
    url = c.asText("url", null);
    frequencySec = c.asInt("frequencySec", 60);
  }


  public UpdaterDataSourceConfig getSourceConfig() { return source; }

  public String getUrl() { return url; }

  public int getFrequencySec() { return frequencySec; }
}
