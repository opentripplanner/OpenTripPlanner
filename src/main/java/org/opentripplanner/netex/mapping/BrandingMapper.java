package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.branding.Branding;
import org.opentripplanner.model.branding.Presentation;
import org.opentripplanner.model.branding.PrivateCode;
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
        PrivateCodeStructure privateCode = branding.getPrivateCode();
        PresentationStructure presentation = branding.getPresentation();

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

        if (privateCode != null) {
            brandingEntity.setPrivateCode(mapPrivateCode(privateCode));
        }

        if (presentation != null) {
            brandingEntity.setPresentation(mapPresentation(presentation));
        }

        return brandingEntity;
    }

    private PrivateCode mapPrivateCode(PrivateCodeStructure privateCodeStructure) {
        PrivateCode privateCode = new PrivateCode();
        privateCode.setType(privateCodeStructure.getType());
        privateCode.setValue(privateCodeStructure.getValue());

        return privateCode;
    }

    private Presentation mapPresentation(PresentationStructure presentationStructure) {
        Presentation presentation = new Presentation();

        presentation.setColorSystem(presentationStructure.getColourSystem());
        presentation.setColor(presentationStructure.getColour());
        presentation.setColorName(presentationStructure.getColourName());
        presentation.setTextColor(presentationStructure.getTextColour());
        presentation.setTextColorName(presentationStructure.getTextColourName());
        presentation.setBackgroundColor(presentationStructure.getBackGroundColour());
        presentation.setBackgroundColorName(presentationStructure.getBackgroundColourName());
        presentation.setTextFont(presentationStructure.getTextFont());
        presentation.setTextFontName(presentationStructure.getTextFontName());
        presentation.setTextLanguage(presentationStructure.getTextLanguage());

        return presentation;
    }
}
