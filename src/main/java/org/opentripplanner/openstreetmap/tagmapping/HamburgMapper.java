package org.opentripplanner.openstreetmap.tagmapping;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * Modified mapper to allow through traffic for combination access=customers and customers=HVV.
 *
 * @see GermanyMapper
 * @see OsmTagMapper
 * @see DefaultMapper
 */
public class HamburgMapper extends GermanyMapper {

  @Override
  public boolean isGeneralNoThroughTraffic(OSMWithTags way) {
    String access = way.getTag("access");
    boolean isNoThroughTraffic = doesTagValueDisallowThroughTraffic(access);

    if (isNoThroughTraffic && way.hasTag("customers")) {
      String customers = way.getTag("customers");
      return !isAllowedThroughTrafficForHVV(access, customers);
    }

    return isNoThroughTraffic;
  }

  private boolean isAllowedThroughTrafficForHVV(String access, String customers) {
    boolean isAccessCustomers = "customers".equals(access);
    boolean isHVV = "HVV".equals(customers);
    return isAccessCustomers && isHVV;
  }
}
