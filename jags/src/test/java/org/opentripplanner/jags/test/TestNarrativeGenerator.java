package org.opentripplanner.jags.test;

import junit.framework.TestCase;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.TransportationMode;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.jags.edgetype.loader.NetworkLinker;
import org.opentripplanner.jags.edgetype.loader.ShapefileStreetLoader;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;
import org.opentripplanner.jags.narrative.Narrative;
import org.opentripplanner.jags.narrative.NarrativeSection;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.ShortestPathTree;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Vector;

public class TestNarrativeGenerator extends TestCase {

	Graph graph;
	GtfsContext context;

	public void setUp() {
		try {
			context = GtfsLibrary
					.readGtfs(new File(ConstantsForTests.PORTLAND_GTFS));
			graph = new Graph();
			GTFSHopLoader hl = new GTFSHopLoader(graph, context);
			hl.load();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		NetworkLinker nl = new NetworkLinker(graph);
		nl.createLinkage();
	}

	public void testNarrativeGenerator() {

		Vertex airport = graph.getVertex("TriMet_10579");
		WalkOptions wo = new WalkOptions();
		wo.setGtfsContext(context);
		ShortestPathTree spt = Dijkstra.getShortestPathTree(graph,
				"TriMet_6876", airport.label, new State(new GregorianCalendar(
						2009, 10, 15, 12, 36, 0)), wo);

		GraphPath path = spt.getPath(airport);

		Narrative narrative = new Narrative(path);
		Vector<NarrativeSection> sections = narrative.getSections();
		/*
		 * This trip, from a bus stop near TriMet HQ to the Airport, should take
		 * the #70 bus, then transfer, then take the MAX Red Line, thus three
		 * sections. If the test fails, that's because a more complex (and
		 * wrong) route is being chosen.
		 */
		assertEquals(3, sections.size());
		assertEquals(TransportationMode.BUS, sections.elementAt(0).getMode());
		assertEquals(TransportationMode.TRAM, sections.elementAt(1).getMode());
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
		assertEquals(1, narrative.getSections().size());
		assertEquals(2, narrative.getSections().firstElement().getItems()
				.size());
	}
}
