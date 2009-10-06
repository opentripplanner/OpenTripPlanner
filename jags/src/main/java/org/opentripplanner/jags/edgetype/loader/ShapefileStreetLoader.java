package org.opentripplanner.jags.edgetype.loader;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.DrawHandler;
import org.opentripplanner.jags.edgetype.Street;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;


import java.util.Iterator;

public class ShapefileStreetLoader {

	Graph graph;
	FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
	
	public ShapefileStreetLoader( Graph graph, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource ) {
		this.graph = graph;
		this.featureSource = featureSource;
	}
	
	public void load() throws Exception {
		//fixme: what logger? Logger.log("loading shapes from " + shapefile);
		
		FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource.getFeatures();
		Iterator<SimpleFeature> i=features.iterator();
		 try {
		     while(i.hasNext()){
		          SimpleFeature feature = i.next();
		          Geometry geom = (Geometry) feature.getDefaultGeometry();
		          String name = "" + feature.getAttribute("StreetCode");
		          Street street = new Street(name, geom.getLength());
		          Coordinate[] coordinates = geom.getCoordinates();
		          
		          String trafDir = (String) feature.getAttribute("TrafDir");
		          Vertex startcorner = graph.addVertex(coordinates[0].toString());
		          Vertex endcorner = graph.addVertex(coordinates[coordinates.length - 1].toString());
		          
		          if (trafDir.equals("W")) {
		        	  //traffic flows With geometry
		        	  graph.addEdge(startcorner, endcorner, street);
		          } else if (trafDir.equals("A")) {
		        	  //traffic flows Against geometry
		        	  graph.addEdge(endcorner, startcorner, street);
		          } else if (trafDir.equals("T")) {
		        	  //traffic flows Two ways
		        	  graph.addEdge(startcorner, endcorner, street);
		        	  graph.addEdge(endcorner, startcorner, street);
		          } else {
		        	  //pedestrian
		          }
		          
		     }
		 }
		 finally {
		     features.close( i );
		 }

		
	}
}
