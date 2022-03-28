package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.opentripplanner.netex.mapping.MappingSupport;

public class GroupOfRoutesTest {

    private static final String ID = "Test:GroupOfLines:1";
    private static final String PRIVATE_CODE = "test_private_code";
    private static final String SHORT_NAME = "test_short_name";
    private static final String NAME = "test_name";
    private static final String DESCRIPTION = "description";

    @Test
    public void testToString() {
        GroupOfRoutes groupOfRoutes = new GroupOfRoutes(
                MappingSupport.ID_FACTORY.createId(ID),
                PRIVATE_CODE,
                SHORT_NAME,
                NAME,
                DESCRIPTION
        );

        assertEquals(
                "GroupOfRoutes{"
                        + "id: F:Test:GroupOfLines:1, "
                        + "privateCode: 'test_private_code', "
                        + "shortName: 'test_short_name', "
                        + "name: 'test_name', "
                        + "description: 'description'"
                        + "}",
                groupOfRoutes.toString()
        );
    }

}
