package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Branding;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PresentationStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

/**
 * Responsible for mapping NeTEx Branding into the OTP model.
 */
public class BrandingMapper {

    private final FeedScopedIdFactory idFactory;

    public BrandingMapper(FeedScopedIdFactory idFactory) {
        this.idFactory = idFactory;
    }

    /**
     * Convert NeTEx Branding entity into OTP model.
     *
     * @param branding NeTEx branding entity
     * @return OTP Branding model
     */
    public Branding mapBranding(org.rutebanken.netex.model.Branding branding) {
        final FeedScopedId id = idFactory.createId(branding.getId());

        final String shortName = branding.getShortName() != null ?
                branding.getShortName().getValue()
                : null;

        final String name = branding.getName() != null ?
                branding.getName().getValue()
                : null;

        final String description = branding.getDescription() != null ?
                branding.getDescription().getValue()
                : null;

        final String url = branding.getUrl();

        final String image = branding.getImage();

        return new Branding(id, shortName, name, url, description, image);
    }
}
