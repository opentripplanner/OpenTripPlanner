package org.opentripplanner.gbfs;

import java.net.URI;

public interface GbfsFeedDetails<N> {
  N getName();

  URI getUrl();
}
