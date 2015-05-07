/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.shapefile;
import junit.framework.TestCase;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;


public class CompositeConverterTest extends TestCase {
    
    private CompositeStreetTraversalPermissionConverter converter;
    private StubSimpleFeature stubFeature;
    private CaseBasedTraversalPermissionConverter caseConverter1;
    private CaseBasedTraversalPermissionConverter caseConverter2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        converter = new CompositeStreetTraversalPermissionConverter();

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