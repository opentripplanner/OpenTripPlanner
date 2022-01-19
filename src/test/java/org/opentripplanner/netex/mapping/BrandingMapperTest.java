package org.opentripplanner.netex.mapping;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.branding.Presentation;
import org.opentripplanner.model.branding.PrivateCode;
import org.rutebanken.netex.model.Branding;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.PresentationStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BrandingMapperTest {

    private final String ID = "RUT:Branding:1";
    private final String NAME = "test_name";
    private final String SHORT_NAME = "test_short_name";
    private final String DESCRIPTION = "test_description";
    private final String URL = "test_url";
    private final String IMAGE = "test_image";

    private final String PRIVATE_CODE_TYPE = "test_type";
    private final String PRIVATE_CODE_VALUE = "test_privateCode";

    private final String COLOR_SYSTEM = "test_system";
    private final byte[] COLOR = new byte[]{0, 0, 0};
    private final String COLOR_NAME = "test_color_name";
    private final byte[] TEXT_COLOR = new byte[]{1, 1, 1};
    private final String TEXT_COLOR_NAME = "test_text_color_name";
    private final byte[] BACKGROUND_COLOR = new byte[]{2, 2, 2};
    private final String BACKGROUND_COLOR_NAME = "test_background_color_name";
    private final String TEXT_FONT = "test_text_font";
    private final String TEXT_FONT_NAME = "test_text_font_name";
    private final String TEXT_LANGUAGE = "test_text_language";

    @Test
    public void mapBranding() {
        BrandingMapper brandingMapper = new BrandingMapper(MappingSupport.ID_FACTORY);

        org.opentripplanner.model.branding.Branding branding =
                brandingMapper.mapBranding(createBranding());

        assertEquals(ID, branding.getId().getId());
        assertEquals(NAME, branding.getName());
        assertEquals(SHORT_NAME, branding.getShortName());
        assertEquals(DESCRIPTION, branding.getDescription());
        assertEquals(URL, branding.getUrl());
        assertEquals(IMAGE, branding.getImage());

        PrivateCode privateCode = branding.getPrivateCode();
        assertNotNull(privateCode);
        assertEquals(PRIVATE_CODE_TYPE, privateCode.getType());
        assertEquals(PRIVATE_CODE_VALUE, privateCode.getValue());

        Presentation presentation = branding.getPresentation();
        assertNotNull(presentation);
        assertEquals(COLOR_SYSTEM, presentation.getColorSystem());
        assertEquals(COLOR, presentation.getColor());
        assertEquals(COLOR_NAME, presentation.getColorName());
        assertEquals(TEXT_COLOR, presentation.getTextColor());
        assertEquals(TEXT_COLOR_NAME, presentation.getTextColorName());
        assertEquals(BACKGROUND_COLOR, presentation.getBackgroundColor());
        assertEquals(BACKGROUND_COLOR_NAME, presentation.getBackgroundColorName());
        assertEquals(TEXT_FONT, presentation.getTextFont());
        assertEquals(TEXT_FONT_NAME, presentation.getTextFontName());
        assertEquals(TEXT_LANGUAGE, presentation.getTextLanguage());
    }

    public Branding createBranding() {
        PrivateCodeStructure privateCode = new PrivateCodeStructure()
                .withType(PRIVATE_CODE_TYPE)
                .withValue(PRIVATE_CODE_VALUE);

        PresentationStructure presentation = new PresentationStructure()
                .withColourSystem(COLOR_SYSTEM)
                .withColour(COLOR)
                .withColourName(COLOR_NAME)
                .withTextColour(TEXT_COLOR)
                .withTextColourName(TEXT_COLOR_NAME)
                .withBackGroundColour(BACKGROUND_COLOR)
                .withBackgroundColourName(BACKGROUND_COLOR_NAME)
                .withTextFont(TEXT_FONT)
                .withTextFontName(TEXT_FONT_NAME)
                .withTextLanguage(TEXT_LANGUAGE);

        return new Branding()
                .withId(ID)
                .withName(new MultilingualString().withValue(NAME))
                .withShortName(new MultilingualString().withValue(SHORT_NAME))
                .withDescription(new MultilingualString().withValue(DESCRIPTION))
                .withUrl(URL)
                .withImage(IMAGE)
                .withPrivateCode(privateCode)
                .withPresentation(presentation);
    }
}
