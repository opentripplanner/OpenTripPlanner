package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.bike_rental.GbfsBikeRentalDataSource;

public class GbfsSourceConfig extends UpdaterSourceConfig implements
    GbfsBikeRentalDataSource.Parameters {

  private final boolean routeAsCar;

  public GbfsSourceConfig(NodeAdapter c) {
    super(c);
    routeAsCar = c.asBoolean("routeAsCar", false);
  }

  public boolean routeAsCar() { return this.routeAsCar; }
}
