package org.opentripplanner.routing.edgetype.loader;

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

import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

public class TestShapefileLoader extends TestCase {
    
    public void testBasic() throws Exception {
        Graph gg = new Graph();
    
        File file = new File("src/test/resources/nyc_streets/streets.shp");

        if (!file.exists()) {
            System.out.println("No New York City basemap; skipping; see comment here for details");
            /* This test requires the New York City base map, available at:
             * http://www.nyc.gov/html/dcp/html/bytes/dwnlion.shtml
             * Download the MapInfo file.  This must be converted to a ShapeFile.
             * unzip nyc_lion09ami.zip
             * ogr2ogr -f 'ESRI Shapefile' nyc_streets/streets.shp lion/MNLION1.tab
             * ogr2ogr -update -append -f 'ESRI Shapefile'  nyc_streets lion/SILION1.tab -nln streets
             * ogr2ogr -update -append -f 'ESRI Shapefile'  nyc_streets lion/QNLION1.tab -nln streets
             * ogr2ogr -update -append -f 'ESRI Shapefile'  nyc_streets lion/BKLION1.tab -nln streets
             * ogr2ogr -update -append -f 'ESRI Shapefile'  nyc_streets lion/BXLION1.tab -nln streets
             * 
             * It also requires the NYC Subway data in GTFS:
             * cd src/test/resources
             * wget http://data.topplabs.org/data/mta_nyct_subway/subway.zip
             */
            return;
        }
        
        ShapefileDataStore dataStore = new ShapefileDataStore(file.toURI().toURL());

        String typeNames[] = dataStore.getTypeNames();
        String typeName = typeNames[0];
        
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = dataStore.getFeatureSource(typeName);
        
        
        ShapefileStreetLoader loader = new ShapefileStreetLoader(gg, featureSource);
        loader.load();
        
        System.out.println("vertices: " + gg.getVertices().size());

        GtfsContext context = GtfsLibrary.readGtfs(new File(ConstantsForTests.NY_GTFS));

        GTFSPatternHopLoader hl = new GTFSPatternHopLoader(gg, context);
        hl.load();

        NetworkLinker nl = new NetworkLinker(gg);
        nl.createLinkage();

        System.out.println("vertices: " + gg.getVertices().size());
        Vertex start = gg.getVertex("PARK PL at VANDERBILT AV");
        Vertex end = gg.getVertex("GRAND ST at LAFAYETTE ST");
       
        assertNotNull(start);
        assertNotNull(end);

        ShortestPathTree spt = Dijkstra.getShortestPathTree(gg, 
                   start.getLabel(), 
                   end.getLabel(), 
                   new State(new GregorianCalendar(2009,8,7,12,2,0).getTimeInMillis()), 
                   new TraverseOptions(context));
        GraphPath path = spt.getPath(end);

        assertNotNull(path);

    }
}
