package org.opentripplanner.routing.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class TraverseModeSetTest {

    @Test
    public void testCarMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.CAR);
        
        assertTrue(modeSet.getCar());
        assertTrue(modeSet.getDriving());
        assertFalse(modeSet.getCustomMotorVehicle());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getTrainish());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getBicycle());        
    }

    @Test
    public void testCustomVehicleMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.CUSTOM_MOTOR_VEHICLE);
        
        assertTrue(modeSet.getCustomMotorVehicle());
        assertTrue(modeSet.getDriving());
        assertFalse(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getTrainish());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getBicycle());
    }

    @Test
    public void testWalkMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.WALK);
        
        assertTrue(modeSet.getWalk());
        assertFalse(modeSet.getCustomMotorVehicle());
        assertFalse(modeSet.getDriving());
        assertFalse(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getTrainish());
        assertFalse(modeSet.getBicycle());
    }
    
    @Test
    public void testBikeMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.BICYCLE);

        assertTrue(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCustomMotorVehicle());
        assertFalse(modeSet.getDriving());
        assertFalse(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getTrainish());
        assertFalse(modeSet.getWalk());
    }

}
