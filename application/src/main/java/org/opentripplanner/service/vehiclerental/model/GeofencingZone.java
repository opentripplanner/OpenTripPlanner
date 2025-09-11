package org.opentripplanner.service.vehiclerental.model;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A geometry that describes descriptions about traversing with a rental vehicle or dropping it off
 * inside of it.
 */
public record GeofencingZone(
  FeedScopedId id,
  I18NString name,
  Geometry geometry,
  boolean dropOffBanned,
  boolean traversalBanned
) {
  /**
   * Are there any restrictions in this zone. (It's possible that the data says there are none.)
   */
  public boolean hasRestriction() {
    return dropOffBanned || traversalBanned;
  }

  /**
   * Some GBFS geofencing zones allow both drop off and traversal. In such a case it is interpreted
   * as describing the general business area of an operator. If this is the case you're allowed to
   * travel inside the zone but cannot leave it.
   * <p>
   * The GBFS spec is remarkably thin in this respect:
   * https://github.com/MobilityData/gbfs/blob/master/gbfs.md#geofencing_zonesjson
   */
  public boolean isBusinessArea() {
    return !dropOffBanned && !traversalBanned;
  }
}
