package org.opentripplanner.standalone.config.updaters.sources;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.bike_park.KmlBikeParkDataSource;

public class KmlBikeParkSourceParameters extends UpdaterSourceParameters
    implements KmlBikeParkDataSource.Parameters {

  private final String namePrefix;
  private final boolean zip;

  public KmlBikeParkSourceParameters(NodeAdapter c) {
    super(c);
    namePrefix = c.asText("namePrefix", null);
    zip = c.asBoolean("zip", false);
  }


  public String getNamePrefix() { return namePrefix; }

  public boolean zip() { return zip; }
}
