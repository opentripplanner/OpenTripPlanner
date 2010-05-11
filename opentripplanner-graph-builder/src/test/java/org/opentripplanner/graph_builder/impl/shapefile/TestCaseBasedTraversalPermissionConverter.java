package org.opentripplanner.graph_builder.impl.shapefile;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

import junit.framework.TestCase;


public class TestCaseBasedTraversalPermissionConverter extends TestCase {
    /*
     * Test for ticket #273: ttp://opentripplanner.org/ticket/273
     */
    public void testDefaultValueForNullEntry() throws Exception {
        StubSimpleFeature feature = new StubSimpleFeature();
        feature.addAttribute("DIRECTION", null);
        
        CaseBasedTraversalPermissionConverter converter = new CaseBasedTraversalPermissionConverter();
        converter.setDefaultPermission(StreetTraversalPermission.PEDESTRIAN);
        
        converter.addPermission("FOO", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        
        assertEquals(new P2<StreetTraversalPermission>(StreetTraversalPermission.PEDESTRIAN, StreetTraversalPermission.PEDESTRIAN), converter.convert(feature));
    }
}
