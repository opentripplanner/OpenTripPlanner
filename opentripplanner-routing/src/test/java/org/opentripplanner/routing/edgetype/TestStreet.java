package org.opentripplanner.routing.edgetype;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Vector;

import junit.framework.TestCase;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.narrative.Narrative;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.loader.ShapefileStreetLoader;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class TestStreet extends TestCase {

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

        Edge garfieldPlBack = eastVertex.outgoing.get(0);
        TraverseOptions toWalk = new TraverseOptions(TraverseMode.WALK);
        assertNotNull(garfieldPlBack.traverse(new State(), toWalk));
        
        TraverseOptions toCar = new TraverseOptions(TraverseMode.CAR);
        assertNull(garfieldPlBack.traverse(new State(), toCar));
        
        Edge garfieldPl = eastVertex.incoming.get(0);
        assertNotNull(garfieldPl.traverse(new State(), toCar));
        
        
        ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, northVertex.label,
                eastVertex.label, new State(new GregorianCalendar(2009, 8, 7, 12, 0, 0)
                        .getTimeInMillis()), new TraverseOptions());

        GraphPath path = spt.getPath(eastVertex);
        assertNotNull(path);

        Vector<SPTEdge> edges = path.edges;
        SPTEdge seventhAve = edges.elementAt(0);
        assertEquals("south", seventhAve.payload.getDirection());
        SPTEdge sptGarfieldPl = edges.elementAt(1);
        assertEquals("east", sptGarfieldPl.payload.getDirection());

        Narrative narrative = new Narrative(path);
        String direction = narrative.getSections().elementAt(0).getDirection();
        assertEquals("southeast", direction);
    }
}
