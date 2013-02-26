package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.*;

import org.junit.Test;

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
    public void testAllows() {
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
    }
}
