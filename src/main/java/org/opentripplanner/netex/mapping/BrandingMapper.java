package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Branding;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PresentationStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

/** Responsible for mapping NeTEx Branding into the OTP model. */
public class BrandingMapper {

    private final FeedScopedIdFactory idFactory;

    public BrandingMapper(FeedScopedIdFactory idFactory) {
        this.idFactory = idFactory;
    }

    /**
     * Convert NeTEx Branding entity into OTP model.
     * @param branding NeTEx branding entity
     * @return OTP Branding model
     */
    public Branding mapBranding(org.rutebanken.netex.model.Branding branding) {
        Branding brandingEntity = new Branding(idFactory.createId(branding.getId()));

        MultilingualString shortName = branding.getShortName();
        MultilingualString name = branding.getName();
        MultilingualString description = branding.getDescription();

        brandingEntity.setUrl(branding.getUrl());
        brandingEntity.setImage(branding.getImage());

        if (shortName != null) {
            brandingEntity.setShortName(shortName.getValue());
        }

        if (name != null) {
            brandingEntity.setName(name.getValue());
        }

        if (description != null) {
            brandingEntity.setDescription(description.getValue());
        }

        return brandingEntity;
    }
}
