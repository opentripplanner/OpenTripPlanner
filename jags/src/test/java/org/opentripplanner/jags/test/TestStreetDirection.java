package org.opentripplanner.jags.test;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.jags.algorithm.Dijkstra;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.edgetype.loader.ShapefileStreetLoader;
import org.opentripplanner.jags.spt.GraphPath;
import org.opentripplanner.jags.spt.SPTEdge;
import org.opentripplanner.jags.spt.ShortestPathTree;

import junit.framework.TestCase;

public class TestStreetDirection extends TestCase {

	public void testStreetDirection() {

		Graph gg = new Graph();
		try {
			File file = new File("simple_streets.shp");
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
				if (northVertex == null || sv.getCoordinate().y > northVertex.getCoordinate().y) {
					northVertex = sv;
				}
			}
		}

		SpatialVertex eastVertex = null;
		for (Vertex v : gg.getVertices()) {
			if (v instanceof SpatialVertex) {
				SpatialVertex sv = (SpatialVertex) v;
				if (eastVertex == null || sv.getCoordinate().x > eastVertex.getCoordinate().x) {
					eastVertex = sv;
				}
			}
		}		
		
		ShortestPathTree spt = Dijkstra.getShortestPathTree(gg,
				northVertex.label, eastVertex.label, new State(
						new GregorianCalendar(2009, 8, 7, 12, 0, 0)),
				new WalkOptions());
		
		GraphPath path = spt.getPath(eastVertex);
		assertNotNull(path);
		
		Vector<SPTEdge> edges = path.edges;
		SPTEdge seventhAve = edges.elementAt(0);
		assertEquals("south", seventhAve.payload.getDirection());
		SPTEdge garfieldPl = edges.elementAt(1);
		assertEquals("east", garfieldPl.payload.getDirection());
	}

}
