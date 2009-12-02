package org.opentripplanner.routing.edgetype;

import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class TestStreet extends TestCase {

    private static GeometryFactory _geomFactory = new GeometryFactory();

    public void testStreetWalk() {
        Graph gg = new Graph();

        Vertex start = gg.addVertex("start", -74.002, 40.5);
        Vertex end = gg.addVertex("end", -74.004, 40.5);

        TraverseOptions wo = new TraverseOptions();
        wo.speed = ConstantsForTests.WALKING_SPEED;
        double streetLength = 100; // meters
        Edge ee = new Street(start, end, streetLength);
        gg.addEdge(ee);

        // Start at October 21, 2009 at 1:00:00pm
        GregorianCalendar startTime = new GregorianCalendar(2009, 9, 21, 13, 0, 0);
        GregorianCalendar endTime = (GregorianCalendar) startTime.clone();
        int expectedSecElapsed = (int) (streetLength / wo.speed);
        endTime.add(GregorianCalendar.SECOND, expectedSecElapsed);

        State s0 = new State(startTime.getTimeInMillis());
        TraverseResult wr = ee.traverse(s0, wo);

        assertNotNull(wr);
        assertEquals(wr.weight, streetLength / wo.speed);
        // Has the time elapsed as expected?
        assertEquals(wr.state.getTime(), endTime.getTimeInMillis());

        wr = null;
        s0 = new State(endTime.getTimeInMillis());
        wr = ee.traverseBack(s0, wo);

        assertNotNull(wr);
        assertEquals(wr.weight, streetLength / wo.speed);
        assertEquals(wr.state.getTime(), startTime.getTimeInMillis());
    }

    public void testStreetDirection() {

        Street streetN = createStreet(47.670300419806246, -122.28909730911255, 47.67212102588157,
                -122.28914022445679);
        assertEquals("north", streetN.getDirection());

        Street streetNE = createStreet(47.670300419806246, -122.28909730911255, 47.67209212786854,
                -122.28796005249023);
        assertEquals("northeast", streetNE.getDirection());

        Street streetE = createStreet(47.670300419806246, -122.28909730911255, 47.670285970297634,
                -122.28796005249023);
        assertEquals("east", streetE.getDirection());

        Street streetSE = createStreet(47.670300419806246, -122.28909730911255, 47.66850865023101,
                -122.28802442550659);
        assertEquals("southeast", streetSE.getDirection());

        Street streetS = createStreet(47.670300419806246, -122.28909730911255, 47.668494200226334,
                -122.28914022445679);
        assertEquals("south", streetS.getDirection());

        Street streetSW = createStreet(47.670300419806246, -122.28909730911255, 47.66855200022105,
                -122.2901701927185);
        assertEquals("southwest", streetSW.getDirection());

        Street streetW = createStreet(47.670300419806246, -122.28909730911255, 47.670300419806246,
                -122.29019165039062);
        assertEquals("west", streetW.getDirection());

        Street streetNW = createStreet(47.670300419806246, -122.28909730911255, 47.67214992387863,
                -122.29019165039062);
        assertEquals("northwest", streetNW.getDirection());
    }

    private Street createStreet(double latFrom, double lonFrom, double latTo, double lonTo) {
        Vertex from = new GenericVertex("from", lonFrom, latFrom);
        Vertex to = new GenericVertex("to", lonTo, latTo);
        double len = DistanceLibrary.distance(latFrom, lonFrom, latTo, lonTo);
        Street street = new Street(from, to, len);
        LineString line = _geomFactory.createLineString(new Coordinate[] { from.getCoordinate(),
                to.getCoordinate() });
        street.setGeometry(line);
        return street;
    }
}
