package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.model.Branding;

import javax.annotation.Nullable;

/** Responsible for mapping GTFS Route into the OTP Branding model. */
public class BrandingMapper {

    /**
     * Convert GTFS Route entity into OTP Branding model.
     * @param route GTFS Route entity
     * @return OTP branding model. Null if route branding url is not present.
     */
    @Nullable
    public Branding map(Route route) {
        if (route.getBrandingUrl() == null) {
            return null;
        }

        Branding branding = new Branding();
        branding.setUrl(route.getBrandingUrl());

        return branding;
    }
}
