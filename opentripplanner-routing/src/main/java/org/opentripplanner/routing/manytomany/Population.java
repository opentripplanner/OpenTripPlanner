package org.opentripplanner.routing.manytomany;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.DefaultQuery;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A collection of individual locations that will be used as either the origin
 * set or the destination set in a many-to-many search.
 * 
 * @author andrewbyrd
 */
public class Population {
	// private static Logger LOG = LoggerFactory.getLogger(Population.class);
	List<Individual> elements;

	public Population() {
		elements = new ArrayList<Individual>();
	}

	public static Population fromCSV(String filename) {
		Population ret = new Population();
		// Individual i = new Individual();
		// ret.add(i);
		return ret;
	}


	public static Population fromShapefile(String filename, String attribute) {
		Population population = new Population();
		System.out.printf("Loading field '%s' from shapefile %s\n", attribute,
				filename);
		try {
			File file = new File(filename);
			FileDataStore store = FileDataStoreFinder.getDataStore(file);
			FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = store
					.getFeatureSource();

			CoordinateReferenceSystem sourceCRS = featureSource.getInfo()
					.getCRS();
			Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER,
					Boolean.TRUE);
			CRSAuthorityFactory factory = ReferencingFactoryFinder
					.getCRSAuthorityFactory("EPSG", hints);
			CoordinateReferenceSystem worldCRS = factory
					.createCoordinateReferenceSystem("EPSG:4326");

			DefaultQuery query = new DefaultQuery();
			query.setCoordinateSystem(sourceCRS);
			query.setCoordinateSystemReproject(worldCRS);

			FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource
					.getFeatures(query);
			Iterator<SimpleFeature> it = features.iterator();
			int i = 0;
			while (it.hasNext()) {
				SimpleFeature feature = it.next();
				Geometry geom = (Geometry) feature.getDefaultGeometry();
				Point point = null;
				if (geom instanceof Point) {
					point = (Point) geom;
				} else if (geom instanceof Polygon) {
					point = ((Polygon) geom).getCentroid();
				} else {
					throw new IllegalStateException(
							"Shapefile must contain either points or polygons.");
				}
				double data = (Double) feature.getAttribute(attribute);
				// System.out.printf("%5.2f\t%s\n", data, point.toString());
				Vertex vertex = new GenericVertex("pop_shp_" + i, point.getX(),
						point.getY());
				Individual individual = new Individual(vertex, data);
				population.elements.add(individual);
				i++;
			}
			features.close(it);
		} catch (Exception ex) {
			throw new IllegalStateException(
					"Error loading population from shapefile ", ex);
		}

		System.out.printf("Done loading shapefile.\n");
		return population;
	}

	public void clearResults() {
		for (Individual i : elements) {
			i.result = Double.POSITIVE_INFINITY;
		}
	}

	public void dump() {
		for (Individual i : elements) {
			System.out.printf("%s %f %f\n", i.vertex, i.data, i.result);
		}
	}

	public void link(Graph graph) {
		StreetVertexIndexServiceImpl index = new StreetVertexIndexServiceImpl(
				graph);
		index.setup();
		TraverseOptions opt = new TraverseOptions();
		System.out.printf("Linking population to main graph. \n");
		for (Individual i : elements) {
			Vertex v = i.vertex;
			Vertex close = index.getClosestVertex(new Coordinate(v.getX(), v
					.getY()), opt);
			graph.addEdge(new FreeEdge(v, close));
			graph.addEdge(new FreeEdge(close, v));
			if (close instanceof StreetLocation) {
				// temp vertex with its own extra edges
				for (DirectEdge de : ((StreetLocation) close).getExtra()) {
					graph.addEdge(de);
				}
			}
		}
	}

}
