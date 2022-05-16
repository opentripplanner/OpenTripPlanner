package org.opentripplanner.gtfs.mapping;

import javax.annotation.Nullable;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.organization.Branding;

/** Responsible for mapping GTFS Route into the OTP Branding model. */
public class BrandingMapper {

  /**
   * Convert GTFS Route entity into OTP Branding model.
   *
   * @param route GTFS Route entity
   * @return OTP branding model. Null if route branding url is not present.
   */
  @Nullable
  public Branding map(Route route) {
    if (route.getBrandingUrl() == null) {
      return null;
    }
    // Make an id from the url, the id is required
    var id = new FeedScopedId(route.getId().getAgencyId(), route.getBrandingUrl());
    return Branding.of(id).setUrl(route.getBrandingUrl()).build();
  }
}
