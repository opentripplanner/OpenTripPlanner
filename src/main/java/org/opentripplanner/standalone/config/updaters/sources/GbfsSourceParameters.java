package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.bike_rental.GbfsBikeRentalDataSource;

public class GbfsSourceParameters extends UpdaterSourceParameters implements
    GbfsBikeRentalDataSource.Parameters {

  private final boolean routeAsCar;

  public GbfsSourceParameters(NodeAdapter c) {
    super(c);
    routeAsCar = c.asBoolean("routeAsCar", false);
  }

  public boolean routeAsCar() { return this.routeAsCar; }
}
