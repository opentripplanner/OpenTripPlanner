package org.opentripplanner.routing.core;

import static org.junit.Assert.*;
import static org.opentripplanner.routing.core.TraverseMode.BICYCLE;
import static org.opentripplanner.routing.core.TraverseMode.BUS;
import static org.opentripplanner.routing.core.TraverseMode.CAR;
import static org.opentripplanner.routing.core.TraverseMode.SHARE_TAXI;
import static org.opentripplanner.routing.core.TraverseMode.TRANSIT;
import static org.opentripplanner.routing.core.TraverseMode.TROLLEY;
import static org.opentripplanner.routing.core.TraverseMode.WALK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class TraverseModeSetTest {

    @Test
    public void testCarMode() {
        TraverseModeSet modeSet = new TraverseModeSet(CAR);
        
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
        TraverseModeSet modeSet = new TraverseModeSet(WALK);
        
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
        TraverseModeSet modeSet = new TraverseModeSet(BICYCLE);

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
        TraverseModeSet modeSet = new TraverseModeSet(SHARE_TAXI);

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
        TraverseModeSet modeSet = new TraverseModeSet(TROLLEY);

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
        TraverseModeSet modeSet = new TraverseModeSet(TRANSIT);

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


    @Test
    public void testBusLikeTransport()
    {
        TraverseModeSet modeSet;
        modeSet = new TraverseModeSet(TRANSIT);
        assertTrue(modeSet.contains(TraverseMode.BUS_TYPE_TRANSPORT));

        List<TraverseMode> arr = new ArrayList<>(Arrays.asList(TraverseMode.values()));
        arr.remove(BUS);
        arr.remove(TROLLEY);
        arr.remove(SHARE_TAXI);
        arr.remove(TRANSIT);
        modeSet = new TraverseModeSet(arr);
        assertFalse(modeSet.contains(TraverseMode.BUS_TYPE_TRANSPORT));

        modeSet = new TraverseModeSet(BUS);
        assertTrue(modeSet.contains(TraverseMode.BUS_TYPE_TRANSPORT));

        modeSet = new TraverseModeSet(TROLLEY);
        assertTrue(modeSet.contains(TraverseMode.BUS_TYPE_TRANSPORT));

        modeSet = new TraverseModeSet(SHARE_TAXI);
        assertTrue(modeSet.contains(TraverseMode.BUS_TYPE_TRANSPORT));
    }
}
