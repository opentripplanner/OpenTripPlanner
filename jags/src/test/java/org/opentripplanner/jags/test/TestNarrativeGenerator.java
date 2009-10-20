package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.loader.ShapefileStreetLoader;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.narrative.Narrative;
import org.opentripplanner.jags.narrative.NarrativeSection;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Vector;

public class TestNarrativeGenerator extends TestCase {

	Graph graph;
	GtfsContext context;

	public void setUp() {
		graph = ConstantsForTests.getInstance().getPortlandGraph();
		context = ConstantsForTests.getInstance().getPortlandContext();
	}

	public void testNarrativeGenerator() {

		Vertex airport = graph.getVertex("TriMet_10579");
		WalkOptions wo = new WalkOptions();
		wo.setGtfsContext(context);
		GregorianCalendar startTime = new GregorianCalendar(2009, 10, 15, 12,
				36, 0);
		ShortestPathTree spt = Dijkstra.getShortestPathTree(graph,
				"TriMet_6876", airport.label, new State(startTime), wo);

		GraphPath path = spt.getPath(airport);

		assertNotNull(path);

		Narrative narrative = new Narrative(path);
		Vector<NarrativeSection> sections = narrative.getSections();
		/*
		 * This trip, from a bus stop near TriMet HQ to the Airport, should take
		 * the #70 bus, then transfer, then take the MAX Red Line, thus three
		 * sections. If the test fails, that's because a more complex (and
		 * wrong) route is being chosen.
		 */

		NarrativeSection busSection = sections.elementAt(0);
		NarrativeSection redLineSection = sections.elementAt(2);

		assertTrue(busSection.getEndTime()
				.before(redLineSection.getStartTime()));
		assertEquals(startTime, busSection.getStartTime());

		assertEquals(TransportationMode.BUS, busSection.getMode());
		assertEquals(TransportationMode.TRAM, redLineSection.getMode());

		assertEquals(3, sections.size());
	}

	public void testWalkNarrative() {

		Graph gg = new Graph();
		try {
			File file = new File(
					"src/test/resources/simple_streets/simple_streets.shp");
			DataStore dataStore = new ShapefileDataStore(file.toURI().toURL());
			// we are now connected
			String[] typeNames = dataStore.getTypeNames();
			String typeName = typeNames[0];

			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

			featureSource = dataStore.getFeatureSource(typeName);

			ShapefileStreetLoader loader = new ShapefileStreetLoader(gg,
					featureSource);
			loader.load();
		} catch (Exception e) {
			e.printStackTrace();
			assertNull("got an exception");
		}

		SpatialVertex northVertex = null;
		for (Vertex v : gg.getVertices()) {
			if (v instanceof SpatialVertex) {
				SpatialVertex sv = (SpatialVertex) v;
				if (northVertex == null
						|| sv.getCoordinate().y > northVertex.getCoordinate().y) {
					northVertex = sv;
				}
			}
		}

		SpatialVertex eastVertex = null;
		for (Vertex v : gg.getVertices()) {
			if (v instanceof SpatialVertex) {
				SpatialVertex sv = (SpatialVertex) v;
				if (eastVertex == null
						|| sv.getCoordinate().x > eastVertex.getCoordinate().x) {
					eastVertex = sv;
				}
			}
		}

		ShortestPathTree spt = Dijkstra.getShortestPathTree(gg,
				northVertex.label, eastVertex.label, new State(
						new GregorianCalendar(2009, 8, 7, 12, 0, 0)),
				new WalkOptions());

		GraphPath path = spt.getPath(eastVertex);
		Narrative narrative = new Narrative(path);

		// there's only one narrative section (the walk), and it has two items
		// (the street segments)
		assertEquals(1, narrative.getSections().size());
		NarrativeSection walkSection = narrative.getSections().firstElement();
		assertEquals(2, walkSection.getItems().size());

		// the geometry starts at the start point, and ends at the end point
		Geometry g = walkSection.getGeometry();
		Coordinate[] coords = g.getCoordinates();

		assertEquals(coords[0], northVertex.getCoordinate());
		assertEquals(coords[coords.length - 1], eastVertex.getCoordinate());

	}
}
