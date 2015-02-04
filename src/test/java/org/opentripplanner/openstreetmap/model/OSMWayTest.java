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

package org.opentripplanner.openstreetmap.model;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.impl.osm.OSMFilter;
import org.opentripplanner.graph_builder.impl.osm.WayProperties;
import org.opentripplanner.graph_builder.impl.osm.WayPropertySet;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

public class OSMWayTest {

    @Test
    public void testIsBicycleDismountForced() {
        OSMWay way = new OSMWay();
        assertFalse(way.isBicycleDismountForced());

        way.addTag("bicycle", "dismount");
        assertTrue(way.isBicycleDismountForced());
    }

    @Test
    public void testIsSteps() {
        OSMWay way = new OSMWay();
        assertFalse(way.isSteps());

        way.addTag("highway", "primary");
        assertFalse(way.isSteps());

        way.addTag("highway", "steps");
        assertTrue(way.isSteps());
    }

    @Test
    public void testIsRoundabout() {
        OSMWay way = new OSMWay();
        assertFalse(way.isRoundabout());

        way.addTag("junction", "dovetail");
        assertFalse(way.isRoundabout());

        way.addTag("junction", "roundabout");
        assertTrue(way.isRoundabout());
    }

    @Test
    public void testIsOneWayDriving() {
        OSMWay way = new OSMWay();
        assertFalse(way.isOneWayForwardDriving());
        assertFalse(way.isOneWayReverseDriving());

        way.addTag("oneway", "notatagvalue");
        assertFalse(way.isOneWayForwardDriving());
        assertFalse(way.isOneWayReverseDriving());

        way.addTag("oneway", "1");
        assertTrue(way.isOneWayForwardDriving());
        assertFalse(way.isOneWayReverseDriving());

        way.addTag("oneway", "-1");
        assertFalse(way.isOneWayForwardDriving());
        assertTrue(way.isOneWayReverseDriving());
    }

    @Test
    public void testIsOneWayBicycle() {
        OSMWay way = new OSMWay();
        assertFalse(way.isOneWayForwardBicycle());
        assertFalse(way.isOneWayReverseBicycle());

        way.addTag("oneway:bicycle", "notatagvalue");
        assertFalse(way.isOneWayForwardBicycle());
        assertFalse(way.isOneWayReverseBicycle());

        way.addTag("oneway:bicycle", "1");
        assertTrue(way.isOneWayForwardBicycle());
        assertFalse(way.isOneWayReverseBicycle());

        way.addTag("oneway:bicycle", "-1");
        assertFalse(way.isOneWayForwardBicycle());
        assertTrue(way.isOneWayReverseBicycle());
    }

    @Test
    public void testIsOneDirectionSidepath() {
        OSMWay way = new OSMWay();
        assertFalse(way.isForwardDirectionSidepath());
        assertFalse(way.isReverseDirectionSidepath());

        way.addTag("bicycle:forward", "use_sidepath");
        assertTrue(way.isForwardDirectionSidepath());
        assertFalse(way.isReverseDirectionSidepath());

        way.addTag("bicycle:backward", "use_sidepath");
        assertTrue(way.isForwardDirectionSidepath());
        assertTrue(way.isReverseDirectionSidepath());
    }

    @Test
    public void testIsOpposableCycleway() {
        OSMWay way = new OSMWay();
        assertFalse(way.isOpposableCycleway());

        way.addTag("cycleway", "notatagvalue");
        assertFalse(way.isOpposableCycleway());

        way.addTag("cycleway", "oppo");
        assertFalse(way.isOpposableCycleway());

        way.addTag("cycleway", "opposite");
        assertTrue(way.isOpposableCycleway());

        way.addTag("cycleway", "nope");
        way.addTag("cycleway:left", "opposite_side");
        assertTrue(way.isOpposableCycleway());
    }

    private P2<StreetTraversalPermission> getWayProperties(OSMWay way) {
        WayPropertySet wayPropertySet = new WayPropertySet();
        WayProperties wayData = wayPropertySet.getDataForWay(way);

        StreetTraversalPermission permissions = OSMFilter.getPermissionsForWay(way,
                wayData.getPermission(), null);
        return OSMFilter.getPermissions(permissions,
                way);
    }

    @Test
    public void testSidepathPermissions() {
        OSMWay way = new OSMWay();
        way.addTag("bicycle", "use_sidepath");
        way.addTag("highway", "primary");
        way.addTag("lanes", "2");
        way.addTag("maxspeed", "70");
        way.addTag("oneway", "yes");
        P2<StreetTraversalPermission> permissionPair = getWayProperties(way);

        assertFalse(permissionPair.first.allows(StreetTraversalPermission.BICYCLE));
        assertFalse(permissionPair.second.allows(StreetTraversalPermission.BICYCLE));

        assertTrue(permissionPair.first.allows(StreetTraversalPermission.CAR));
        assertFalse(permissionPair.second.allows(StreetTraversalPermission.CAR));

        way = new OSMWay();
        way.addTag("bicycle:forward", "use_sidepath");
        way.addTag("highway", "tertiary");
        permissionPair = getWayProperties(way);

        assertFalse(permissionPair.first.allows(StreetTraversalPermission.BICYCLE));
        assertTrue(permissionPair.second.allows(StreetTraversalPermission.BICYCLE));

        assertTrue(permissionPair.first.allows(StreetTraversalPermission.CAR));
        assertTrue(permissionPair.second.allows(StreetTraversalPermission.CAR));

        way = new OSMWay();
        way.addTag("bicycle:backward", "use_sidepath");
        way.addTag("highway", "tertiary");
        permissionPair = getWayProperties(way);

        assertTrue(permissionPair.first.allows(StreetTraversalPermission.BICYCLE));
        assertFalse(permissionPair.second.allows(StreetTraversalPermission.BICYCLE));

        assertTrue(permissionPair.first.allows(StreetTraversalPermission.CAR));
        assertTrue(permissionPair.second.allows(StreetTraversalPermission.CAR));

        way = new OSMWay();
        way.addTag("highway", "tertiary");
        way.addTag("oneway", "yes");
        way.addTag("oneway:bicycle", "no");
        permissionPair = getWayProperties(way);

        assertTrue(permissionPair.first.allows(StreetTraversalPermission.BICYCLE));
        assertTrue(permissionPair.second.allows(StreetTraversalPermission.BICYCLE));

        assertTrue(permissionPair.first.allows(StreetTraversalPermission.CAR));
        assertFalse(permissionPair.second.allows(StreetTraversalPermission.CAR));

        way.addTag("bicycle:forward", "use_sidepath");
        permissionPair = getWayProperties(way);
        assertFalse(permissionPair.first.allows(StreetTraversalPermission.BICYCLE));
        assertTrue(permissionPair.second.allows(StreetTraversalPermission.BICYCLE));

        assertTrue(permissionPair.first.allows(StreetTraversalPermission.CAR));
        assertFalse(permissionPair.second.allows(StreetTraversalPermission.CAR));
    }
}
