package org.opentripplanner.jags.edgetype.loader;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Street;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.PrecisionModel;

import java.util.Iterator;

public class ShapefileStreetLoader {

	Graph graph;
	FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

	public ShapefileStreetLoader(Graph graph,
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
		this.graph = graph;
		this.featureSource = featureSource;	
		
	}
	
	public static LineString toLineString(Geometry g) {
		if (g instanceof LineString) {
			return (LineString) g;
		} else if (g instanceof MultiLineString) {
			MultiLineString ml = (MultiLineString) g;
			Coordinate[] coords = ml.getCoordinates();
			GeometryFactory factory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
			return factory.createLineString(coords);
		} else {
			throw new RuntimeException("found a geometry feature that's not a linestring: " + g);
		}
	}

	public void load() throws Exception {
		// fixme: what logger? Logger.log("loading shapes from " + shapefile);

		FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource.getFeatures();
		Iterator<SimpleFeature> i = features.iterator();
		try {
			while (i.hasNext()) {
				SimpleFeature feature = i.next();
				LineString geom = toLineString((Geometry) feature.getDefaultGeometry());
				String id = "" + feature.getAttribute("StreetCode");
				String name = "" + feature.getAttribute("Street");
				Coordinate[] coordinates = geom.getCoordinates();

				String trafDir = (String) feature.getAttribute("TrafDir");
				Vertex startCorner = new SpatialVertex(
						coordinates[0].toString(), 
						coordinates[0].x, 
						coordinates[0].y);
				startCorner = graph.addVertex(startCorner);
				Vertex endCorner = new SpatialVertex(
						coordinates[coordinates.length - 1].toString(),
						coordinates[coordinates.length - 1].x,
						coordinates[coordinates.length - 1].y);
				endCorner = graph.addVertex(endCorner);

				if (trafDir.equals("W")) {
					// traffic flows With geometry
					Street street = new Street(id, name, geom.getLength());
					street.setGeometry(geom);
					graph.addEdge(startCorner, endCorner, street);
				} else if (trafDir.equals("A")) {
					// traffic flows Against geometry
					Street street = new Street(id, name, geom.getLength());
					street.setGeometry(geom);
					graph.addEdge(endCorner, startCorner, street);
				} else if (trafDir.equals("T")) {
					// traffic flows Two ways
					Street street = new Street(id, name, geom.getLength());
					street.setGeometry(geom);
					graph.addEdge(startCorner, endCorner, street);
					Street backStreet = new Street(id, name, geom.getLength());
					street.setGeometry(geom.reverse());
					graph.addEdge(endCorner, startCorner, backStreet);
				} else {
					// pedestrian
				}

			}
		} finally {
			features.close(i);
		}

	}
}
