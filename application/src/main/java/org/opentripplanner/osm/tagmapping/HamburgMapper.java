package org.opentripplanner.osm.tagmapping;

import org.opentripplanner.osm.model.OsmEntity;

/**
 * Modified mapper to allow through traffic for combination access=customers and customers=HVV.
 *
 * @see GermanyMapper
 * @see OsmTagMapper
 *
 * @author Maintained by HBT (geofox-team@hbt.de)
 */
public class HamburgMapper extends GermanyMapper {

  @Override
  public boolean isGeneralNoThroughTraffic(OsmEntity way) {
    String access = way.getTag("access");
    boolean isNoThroughTraffic = doesTagValueDisallowThroughTraffic(access);

    return isNoThroughTraffic && !way.isTag("customers", "HVV");
  }
}
