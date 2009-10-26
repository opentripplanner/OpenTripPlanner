package org.opentripplanner.jags.edgetype.loader;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Street;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.cs.CoordinateSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.PrecisionModel;

import javax.measure.converter.ConversionException;
import javax.measure.converter.UnitConverter;
import javax.measure.unit.SI;

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

        // Create a converter that will take whatever units the shapefile
        // lengths are in (e.g., feet in NYC) and convert to meters.
        UnitConverter metersConverter = null;
        try {
            CoordinateSystem coordinateSystem = featureSource.getInfo().getCRS().getCoordinateSystem();
            metersConverter = coordinateSystem.getAxis(0).getUnit().getConverterTo(SI.METER);
        } catch (ConversionException e) {
            // This will happen when the shapefile is unprojected, e.g., the
            // units are degrees, which can't be directly converted to meters.
            // TODO: (re)project or ? So far this isn't a big issue since all of
            // the street data we have is projected already (the only data in
            // degrees is the fake simple_streets data)
        }
       
	    
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

                double length = geom.getLength();
                // Convert to meters
                if (metersConverter != null) {
                    length = metersConverter.convert(length);
                }

                // TODO: The following assumes the street direction convention used in NYC
                // This code should either be moved or generalized (if possible).
				if (trafDir.equals("W")) {
					// traffic flows With geometry
                    Street street = new Street(id, name, length);
					street.setGeometry(geom);
					graph.addEdge(startCorner, endCorner, street);
				} else if (trafDir.equals("A")) {
					// traffic flows Against geometry
                    Street street = new Street(id, name, length);
					street.setGeometry(geom.reverse());
					graph.addEdge(endCorner, startCorner, street);
				} else if (trafDir.equals("T")) {
					// traffic flows Two ways
                    Street street = new Street(id, name, length);
					street.setGeometry(geom);
					graph.addEdge(startCorner, endCorner, street);
                    Street backStreet = new Street(id, name, length);
					backStreet.setGeometry(geom.reverse());
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
