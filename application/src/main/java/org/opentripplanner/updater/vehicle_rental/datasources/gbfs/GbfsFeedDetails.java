package org.opentripplanner.updater.vehicle_rental.datasources.gbfs;

import java.net.URI;

public interface GbfsFeedDetails<N> {
  N getName();

  URI getUrl();
}
