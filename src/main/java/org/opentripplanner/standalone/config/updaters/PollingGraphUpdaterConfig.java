package org.opentripplanner.standalone.config.updaters;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.UpdaterDataSourceFactory;
import org.opentripplanner.updater.PollingGraphUpdater;
import org.opentripplanner.updater.UpdaterDataSourceParameters;

public class PollingGraphUpdaterConfig
    implements PollingGraphUpdater.PollingGraphUpdaterParameters {

  private final String configRef;

  private final UpdaterDataSourceParameters source;

  private final String url;

  private final int frequencySec;

  public PollingGraphUpdaterConfig(String updaterRef, NodeAdapter c) {
    String sourceType = c.asText("sourceType");
    this.configRef = updaterRef + "." + sourceType;
    this.source = UpdaterDataSourceFactory.createDataSourceParameters(sourceType, c);
    this.url = c.asText("url", null);
    this.frequencySec = c.asInt("frequencySec", 60);
  }

  @Override
  public UpdaterDataSourceParameters getSourceParameters() { return source; }

  @Override
  public String getUrl() { return url; }

  @Override
  public int getFrequencySec() { return frequencySec; }

  @Override
  public String getConfigRef() {
    return configRef;
  }
}
