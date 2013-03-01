package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

public class StreetTraversalPermissionTest {

    @Test
    public void testGetCode() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.ALL_DRIVING;
        StreetTraversalPermission perm2 = StreetTraversalPermission.get(perm1.getCode());
        assertEquals(perm1, perm2);
        
        StreetTraversalPermission perm3 = StreetTraversalPermission.BICYCLE;
        assertFalse(perm1.equals(perm3));
    }
    
    @Test
    public void testAdd() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.CAR;
        StreetTraversalPermission all_driving = perm1.add(StreetTraversalPermission.CUSTOM_MOTOR_VEHICLE);
        assertEquals(StreetTraversalPermission.ALL_DRIVING, all_driving);
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
        assertFalse(perm1.allows(StreetTraversalPermission.CROSSHATCHED));
        assertTrue(perm1.allows(StreetTraversalPermission.CAR));
        assertTrue(perm1.allows(StreetTraversalPermission.CUSTOM_MOTOR_VEHICLE));
        assertTrue(perm1.allows(StreetTraversalPermission.BICYCLE));
        assertTrue(perm1.allows(StreetTraversalPermission.PEDESTRIAN));
        assertTrue(perm1.allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));
        
        StreetTraversalPermission perm = StreetTraversalPermission.ALL_DRIVING;
        assertTrue(perm.allows(StreetTraversalPermission.CUSTOM_MOTOR_VEHICLE));
        assertFalse(perm.allows(StreetTraversalPermission.BICYCLE));
        
        perm = StreetTraversalPermission.BICYCLE_AND_DRIVING;
        assertTrue(perm.allows(StreetTraversalPermission.CUSTOM_MOTOR_VEHICLE));
        assertTrue(perm.allows(StreetTraversalPermission.BICYCLE));
        assertFalse(perm.allows(StreetTraversalPermission.PEDESTRIAN));
    }

    @Test
    public void testAllowsTraverseMode() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.ALL;
        assertTrue(perm1.allows(TraverseMode.CAR));
        assertTrue(perm1.allows(TraverseMode.WALK));
        assertTrue(perm1.allows(TraverseMode.CUSTOM_MOTOR_VEHICLE));
        assertTrue(perm1.allows(TraverseMode.WALK));
        
        // StreetTraversalPermission appears not to be used for public transit.
        assertFalse(perm1.allows(TraverseMode.BUS));
        assertFalse(perm1.allows(TraverseMode.TRAINISH));
        
        StreetTraversalPermission perm = StreetTraversalPermission.ALL_DRIVING;
        assertTrue(perm.allows(TraverseMode.CAR));
        assertTrue(perm.allows(TraverseMode.CUSTOM_MOTOR_VEHICLE));
        assertFalse(perm.allows(TraverseMode.BICYCLE));    
    }

    @Test
    public void testAllowsTraverseModeSet() {
        StreetTraversalPermission perm1 = StreetTraversalPermission.ALL_DRIVING;
        assertTrue(perm1.allows(TraverseModeSet.allModes()));
        assertTrue(perm1.allows(new TraverseModeSet(TraverseMode.CAR,
                TraverseMode.BICYCLE)));
        assertTrue(perm1.allows(new TraverseModeSet(TraverseMode.CUSTOM_MOTOR_VEHICLE,
                TraverseMode.TRAINISH, TraverseMode.FERRY)));
        
        // This one has no driving modes.
        assertFalse(perm1.allows(new TraverseModeSet(TraverseMode.BICYCLE,
                TraverseMode.TRAINISH, TraverseMode.FERRY)));
    }
    
    
}
