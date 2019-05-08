package org.opentripplanner.routing.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class TraverseModeSetTest {

    @Test
    public void testCarMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.CAR);
        
        assertTrue(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getTrolley());
        assertFalse(modeSet.getShareTaxi());
    }

    @Test
    public void testWalkMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.WALK);
        
        assertTrue(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getTrolley());
        assertFalse(modeSet.getShareTaxi());
    }
    
    @Test
    public void testBikeMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.BICYCLE);

        assertTrue(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getTrolley());
        assertFalse(modeSet.getShareTaxi());
    }

    @Test
    public void testTaxiMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.SHARE_TAXI);

        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertTrue(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertTrue(modeSet.getShareTaxi());
        assertFalse(modeSet.getTrolley());
    }

    @Test
    public void testTrolleyMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.TROLLEY);

        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertTrue(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getShareTaxi());
        assertTrue(modeSet.getTrolley());
    }

    @Test
    public void testTransitMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.TRANSIT);

        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertTrue(modeSet.isTransit());
        assertTrue(modeSet.getRail());
        assertTrue(modeSet.getTram());
        assertTrue(modeSet.getSubway());
        assertTrue(modeSet.getFunicular());
        assertTrue(modeSet.getGondola());
        assertTrue(modeSet.getShareTaxi());
        assertTrue(modeSet.getTrolley());
    }

}
