package org.opentripplanner.jags.test;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Vector;

import junit.framework.TestCase;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;
import org.opentripplanner.jags.edgetype.Street;
import org.opentripplanner.jags.edgetype.loader.ShapefileStreetLoader;
import org.opentripplanner.jags.narrative.Narrative;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.SPTEdge;
import org.opentripplanner.jags.spt.ShortestPathTree;

public class TestStreet extends TestCase {

    public void testStreetWalk() {
        Graph gg = new Graph();

        Vertex start = gg.addVertex("start", -74.002, 40.5);
        Vertex end = gg.addVertex("end", -74.004, 40.5);

        WalkOptions wo = new WalkOptions();
        wo.speed = ConstantsForTests.WALKING_SPEED;
        double streetLength = 100; // meters
        Edge ee = gg.addEdge(start, end, new Street(streetLength));

        // Start at October 21, 2009 at 1:00:00pm
        GregorianCalendar startTime = new GregorianCalendar(2009, 9, 21, 13, 0, 0);
        GregorianCalendar endTime = (GregorianCalendar) startTime.clone();
        int expectedSecElapsed = (int) (streetLength / wo.speed);
        endTime.add(GregorianCalendar.SECOND, expectedSecElapsed);

        State s0 = new State(startTime.getTimeInMillis());
        WalkResult wr = ee.walk(s0, wo);

        assertNotNull(wr);
        assertEquals(wr.weight, streetLength / wo.speed);
        // Has the time elapsed as expected?
        assertEquals(wr.state.getTime(), endTime.getTimeInMillis());

        wr = null;
        s0 = new State(endTime.getTimeInMillis());
        wr = ee.walkBack(s0, wo);

        assertNotNull(wr);
        assertEquals(wr.weight, streetLength / wo.speed);
        assertEquals(wr.state.getTime(), startTime.getTimeInMillis());
    }

    public void testStreetDirection() {

        Graph gg = new Graph();
        try {
            File file = new File("src/test/resources/simple_streets/simple_streets.shp");
            DataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
            // we are now connected
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

            featureSource = dataStore.getFeatureSource(typeName);

            ShapefileStreetLoader loader = new ShapefileStreetLoader(gg, featureSource);
            loader.load();
        } catch (Exception e) {
            e.printStackTrace();
            assertNull("got an exception");
        }

        Vertex northVertex = null;
        for (Vertex v : gg.getVertices()) {
            if (northVertex == null || v.getCoordinate().y > northVertex.getCoordinate().y) {
                northVertex = v;
            }
        }

        Vertex eastVertex = null;
        for (Vertex v : gg.getVertices()) {
            if (eastVertex == null || v.getCoordinate().x > eastVertex.getCoordinate().x) {
                eastVertex = v;
            }
        }

        ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, northVertex.label,
                eastVertex.label, new State(new GregorianCalendar(2009, 8, 7, 12, 0, 0)
                        .getTimeInMillis()), new WalkOptions());

        GraphPath path = spt.getPath(eastVertex);
        assertNotNull(path);

        Vector<SPTEdge> edges = path.edges;
        SPTEdge seventhAve = edges.elementAt(0);
        assertEquals("south", seventhAve.payload.getDirection());
        SPTEdge garfieldPl = edges.elementAt(1);
        assertEquals("east", garfieldPl.payload.getDirection());

        Narrative narrative = new Narrative(path);
        String direction = narrative.getSections().elementAt(0).getDirection();
        assertEquals("southeast", direction);
    }
}
