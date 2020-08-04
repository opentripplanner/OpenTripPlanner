package org.opentripplanner.ext.bikerentalservicedirectory;

import org.opentripplanner.updater.UpdaterDataSourceConfig;
import org.opentripplanner.updater.UpdaterDataSourceParameters;

public class GbfsUpdaterSourceConfig implements UpdaterDataSourceConfig {

  private static final String DEFAULT_UPDATER_TYPE = "gbfs";

  private final UpdaterDataSourceParameters updaterSourceParameters;

  public GbfsUpdaterSourceConfig(UpdaterDataSourceParameters sourceParameters) {
    this.updaterSourceParameters = sourceParameters;
  }

  @Override
  public String getType() {
    return DEFAULT_UPDATER_TYPE;
  }

  @Override
  public UpdaterDataSourceParameters getUpdaterSourceParameters() {
    return updaterSourceParameters;
  }
}
