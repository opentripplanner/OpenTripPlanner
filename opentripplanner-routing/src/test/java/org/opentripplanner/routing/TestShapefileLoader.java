package org.opentripplanner.routing;

import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.loader.GTFSHopLoader;
import org.opentripplanner.routing.edgetype.loader.NetworkLinker;
import org.opentripplanner.routing.edgetype.loader.ShapefileStreetLoader;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.GraphPath;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.FeatureSource;
import org.geotools.referencing.CRS;

import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

public class TestShapefileLoader extends TestCase {
    
    public void testBasic() throws Exception {
        Graph gg = new Graph();
        File file = new File("src/test/resources/nyc_streets/streets.shp");

        ShapefileDataStore dataStore = new ShapefileDataStore(file.toURI().toURL());

        String typeNames[] = dataStore.getTypeNames();
        String typeName = typeNames[0];
        
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = dataStore.getFeatureSource(typeName);
        
        
        ShapefileStreetLoader loader = new ShapefileStreetLoader(gg, featureSource);
        loader.load();
        
        System.out.println("vertices: " + gg.getVertices().size());

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.NY_GTFS));

        GTFSHopLoader hl = new GTFSHopLoader(gg, context);
        hl.load();
        
        NetworkLinker nl = new NetworkLinker(gg);
        nl.createLinkage();
                
        System.out.println("vertices: " + gg.getVertices().size());
        Vertex start = gg.getVertex("PARK PL at VANDERBILT AV");
        Vertex end = gg.getVertex("GRAND ST at LAFAYETTE ST");
        //Vertex start = gg.getVertex("6 Ave at W 23rd St");
        //Vertex end = gg.getVertex("5th Ave at E 81st St");
       
        assertNotNull(start);
        assertNotNull(end);
                    
        ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, 
                   start.getLabel(), 
                   end.getLabel(), 
                   new State(new GregorianCalendar(2009,8,7,12,2,0).getTimeInMillis()), 
                   new TraverseOptions(context));
        GraphPath path = spt.getPath(end);
        
        assertNotNull(path);
        SPTEdge street = null;
        for (int i = 0; i < path.edges.size(); ++i) {
        	street = path.edges.get(i);
            System.out.println(street.payload.getName() + ": " + 
            		street.payload.getDistance() + " from " + 
            		street.fromv + " to " + street.tov);
        }
        System.out.println("arrive at: " + street.getToSPTVertex().state.getTime());                 
    }
}
