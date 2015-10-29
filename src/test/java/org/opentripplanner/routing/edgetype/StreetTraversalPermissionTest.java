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

package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

public class StreetTraversalPermissionTest {

    @Test
    public void testGetCode() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.BICYCLE_AND_CAR;
        StreetTraversalPermission perm2 = StreetTraversalPermission.get(perm1.code);
        assertEquals(perm1, perm2);

        StreetTraversalPermission perm3 = StreetTraversalPermission.BICYCLE;
        assertFalse(perm1.equals(perm3));
    }

    @Test
    public void testRemove() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.CAR;
        StreetTraversalPermission none = perm1.remove(StreetTraversalPermission.CAR);
        assertEquals(StreetTraversalPermission.NONE, none);
    }

    @Test
    public void testAllowsStreetTraversalPermission() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.ALL;
        assertTrue(perm1.allows(StreetTraversalPermission.CAR));
        assertTrue(perm1.allows(StreetTraversalPermission.BICYCLE));
        assertTrue(perm1.allows(StreetTraversalPermission.PEDESTRIAN));
        assertTrue(perm1.allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));
    }

    @Test
    public void testAllowsTraverseMode() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.ALL;
        assertTrue(perm1.allows(TraverseMode.CAR));
        assertTrue(perm1.allows(TraverseMode.WALK));

        // StreetTraversalPermission is not used for public transit.
        assertFalse(perm1.allows(TraverseMode.BUS));
        assertFalse(perm1.allows(TraverseMode.TRAINISH));
    }

    @Test
    public void testAllowsTraverseModeSet() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.BICYCLE_AND_CAR;
        assertTrue(perm1.allows(TraverseModeSet.allModes()));
        assertTrue(perm1.allows(new TraverseModeSet(TraverseMode.CAR, TraverseMode.BICYCLE)));
        assertTrue(perm1.allows(new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.TRAINISH, TraverseMode.FERRY)));
        assertFalse(perm1.allows(new TraverseModeSet(TraverseMode.WALK)));
    }

    @Test
    public void testAllowsAnythingNothing() {
        StreetTraversalPermission perm = StreetTraversalPermission.CAR;
        assertTrue(perm.allowsAnything());
        assertFalse(perm.allowsNothing());
        
        perm = StreetTraversalPermission.NONE;
        assertFalse(perm.allowsAnything());
        assertTrue(perm.allowsNothing());
    }

    @Test
    public void testIntersect() {
        StreetTraversalPermission perm = StreetTraversalPermission.ALL;
        StreetTraversalPermission bike_walk = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

        StreetTraversalPermission combined = perm.intersection(bike_walk);

        assertTrue(perm.allows(StreetTraversalPermission.ALL));
        assertTrue(combined.allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));
        assertFalse(combined.allows(StreetTraversalPermission.CAR));
        assertTrue(bike_walk.allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));
    }
}
