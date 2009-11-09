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
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.narrative.Narrative;
import org.opentripplanner.narrative.NarrativeSection;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransportationMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.loader.ShapefileStreetLoader;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class TestNarrativeGenerator extends TestCase {

    Graph graph;

    GtfsContext context;

    public void setUp() {
        graph = ConstantsForTests.getInstance().getPortlandGraph();
        context = ConstantsForTests.getInstance().getPortlandContext();
    }

    public void testNarrativeGenerator() {

        Vertex airport = graph.getVertex("TriMet_10579");
        TraverseOptions wo = new TraverseOptions();
        wo.setGtfsContext(context);
        GregorianCalendar startTime = new GregorianCalendar(2009, 11, 1, 12, 34, 25);
        ShortestPathTree spt = Dijkstra.getShortestPathTree(graph, "TriMet_6876", airport.label,
                new State(startTime.getTimeInMillis()), wo);

        GraphPath path = spt.getPath(airport);

        assertNotNull(path);

        Narrative narrative = new Narrative(path);
        Vector<NarrativeSection> sections = narrative.getSections();
        /*
         * This trip, from a bus stop near TriMet HQ to the Airport, should take the #70 bus, then
         * transfer, then take the MAX Red Line, thus three sections. If the test fails, that's
         * because a more complex (and wrong) route is being chosen.
         */

        NarrativeSection busSection = sections.elementAt(0);
        NarrativeSection redLineSection = sections.elementAt(2);

        assertTrue(busSection.getEndTime() < redLineSection.getStartTime());
        assertEquals(startTime.getTimeInMillis(), busSection.getStartTime());

        assertEquals(TransportationMode.BUS, busSection.getMode());
        assertEquals(TransportationMode.TRAM, redLineSection.getMode());

        assertEquals(3, sections.size());
    }

    public void testWalkNarrative() {

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

        assertNotNull(northVertex);

        Vertex eastVertex = null;
        for (Vertex v : gg.getVertices()) {
            if (eastVertex == null || v.getCoordinate().x > eastVertex.getCoordinate().x) {
                eastVertex = v;
                }

        }

        assertNotNull(eastVertex);

        ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, northVertex.label,
                eastVertex.label, new State(new GregorianCalendar(2009, 8, 7, 12, 0, 0)
                        .getTimeInMillis()), new TraverseOptions());

        GraphPath path = spt.getPath(eastVertex);
        assertNotNull(path);
        Narrative narrative = new Narrative(path);

        // there's only one narrative section (the walk), and it has two items
        // (the street segments)
        assertEquals(1, narrative.getSections().size());
        NarrativeSection walkSection = narrative.getSections().firstElement();
        assertEquals(2, walkSection.getItems().size());

        // the geometry starts at the start point, and ends at the end point
        Geometry g = walkSection.getGeometry();
        Coordinate[] coords = g.getCoordinates();

        assertTrue(coords[0].distance(northVertex.getCoordinate()) < 0.0000001);
        assertTrue(coords[coords.length - 1].distance(eastVertex.getCoordinate()) < 0.0000001);

    }
}
