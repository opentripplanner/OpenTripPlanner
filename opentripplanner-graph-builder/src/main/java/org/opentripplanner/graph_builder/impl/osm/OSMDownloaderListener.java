package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;

public interface OSMDownloaderListener {
  public void handleMapTile(String key, double lat, double lon,
      File pathToMapTile);
}
