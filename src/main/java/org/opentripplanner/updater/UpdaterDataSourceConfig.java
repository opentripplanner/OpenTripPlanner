package org.opentripplanner.updater;

public interface UpdaterDataSourceConfig {

  String getType();

  UpdaterDataSourceParameters getUpdaterSourceParameters();
}
