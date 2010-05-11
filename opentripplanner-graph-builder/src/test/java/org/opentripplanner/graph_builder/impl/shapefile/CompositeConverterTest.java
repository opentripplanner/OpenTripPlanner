package org.opentripplanner.graph_builder.impl.shapefile;
import junit.framework.TestCase;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;


public class CompositeConverterTest extends TestCase {
    
    private CompositeConverter converter;
    private StubSimpleFeature stubFeature;
    private CaseBasedTraversalPermissionConverter caseConverter1;
    private CaseBasedTraversalPermissionConverter caseConverter2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        converter = new CompositeConverter();

        stubFeature = new StubSimpleFeature();
        stubFeature.addAttribute("trafdir", "1");
        stubFeature.addAttribute("streettype", "nicestreet");

        caseConverter1 = new CaseBasedTraversalPermissionConverter("trafdir");
        caseConverter2 = new CaseBasedTraversalPermissionConverter("streettype");        
        converter.add(caseConverter1);
        converter.add(caseConverter2);        
    }

    public void testPermissionAggregationAnd() throws Exception {
        caseConverter1.addPermission("1", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        caseConverter2.addPermission("nicestreet", StreetTraversalPermission.ALL, StreetTraversalPermission.NONE);        
        
        P2<StreetTraversalPermission> result = converter.convert(stubFeature);
        assertEquals(new P2<StreetTraversalPermission>(StreetTraversalPermission.ALL, StreetTraversalPermission.NONE), result);
    }

    public void testPermissionAggregationOr() throws Exception {
        converter.setOrPermissions(true);
        
        caseConverter1.addPermission("1", StreetTraversalPermission.NONE, StreetTraversalPermission.ALL);
        caseConverter2.addPermission("nicestreet", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        
        P2<StreetTraversalPermission> result = converter.convert(stubFeature);
        assertEquals(new P2<StreetTraversalPermission>(StreetTraversalPermission.ALL, StreetTraversalPermission.ALL), result);
    }
    
    public void testWalkingBiking() throws Exception {
        converter.setOrPermissions(true);
        caseConverter1.addPermission("1", StreetTraversalPermission.PEDESTRIAN, StreetTraversalPermission.PEDESTRIAN);
        caseConverter2.addPermission("nicestreet", StreetTraversalPermission.BICYCLE, StreetTraversalPermission.BICYCLE);
        P2<StreetTraversalPermission> result = converter.convert(stubFeature);
        assertEquals(new P2<StreetTraversalPermission>(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE), result);
    }
    
    public void testAllCars() throws Exception {
        caseConverter1.addPermission("1", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);
        caseConverter2.addPermission("nicestreet", StreetTraversalPermission.CAR, StreetTraversalPermission.CAR);
        P2<StreetTraversalPermission> result = converter.convert(stubFeature);
        assertEquals(new P2<StreetTraversalPermission>(StreetTraversalPermission.CAR, StreetTraversalPermission.CAR), result);
    }

}