package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.opentripplanner.netex.mapping.MappingSupport;

public class BrandingTest {

    private static final String ID = "Test:Branding:1";
    private static final String SHORT_NAME = "test_short_name";
    private static final String NAME = "test_name";
    private static final String URL = "test_url";
    private static final String DESCRIPTION = "test_description";
    private static final String IMAGE = "test_image";

    @Test
    public void testToString() {
        Branding branding = new Branding(
                MappingSupport.ID_FACTORY.createId(ID),
                SHORT_NAME,
                NAME,
                URL,
                DESCRIPTION,
                IMAGE
        );

        assertEquals(
                "Branding{"
                        + "id: F:Test:Branding:1, "
                        + "shortName: 'test_short_name', "
                        + "name: 'test_name', "
                        + "url: 'test_url', "
                        + "description: 'test_description', "
                        + "image: 'test_image'"
                        + "}",
                branding.toString()
        );
    }
}
