package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * This graph does a quick sanity check that the GraphQL schema can be built. It's much
 * quicker to run the test than to build a jar and start up OTP.`
 */
public class GraphQLIndexTest {

    @Test
    public void testGraphQLIndex() {
        var schema = LegacyGraphQLIndex.buildSchema();
        assertNotNull(schema);
    }
}
