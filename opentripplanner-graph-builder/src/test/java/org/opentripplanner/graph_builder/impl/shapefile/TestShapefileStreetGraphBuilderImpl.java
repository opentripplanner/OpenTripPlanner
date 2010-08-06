/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.shapefile;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Test;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class TestShapefileStreetGraphBuilderImpl extends TestCase {

    @Test
    public void testBasic() throws Exception {
        Graph gg = new Graph();

        URL resource = getClass().getResource("nyc_streets/streets.shp");
        File file = null;
        if (resource != null) {
            file = new File(resource.getFile());
        }
        if (file == null || !file.exists()) {
            System.out.println("No New York City basemap; skipping; see comment here for details");
            /*
             * This test requires the New York City base map, available at:
             * http://www.nyc.gov/html/dcp/html/bytes/dwnlion.shtml Download the MapInfo file. This
             * must be converted to a ShapeFile. mkdir nyc_streets;
             * unzip nyc_lion10ami.zip
             * ogr2ogr -f 'ESRI Shapefile' nyc_streets/streets.shp lion/MNLION1.tab 
             * ogr2ogr -update -append -f 'ESRI Shapefile' nyc_streets lion/SILION1.tab -nln streets 
             * ogr2ogr -update -append -f 'ESRI Shapefile' nyc_streets lion/QNLION1.tab -nln streets 
             * ogr2ogr -update -append -f 'ESRI Shapefile' nyc_streets lion/BKLION1.tab -nln streets 
             * ogr2ogr -update -append -f 'ESRI Shapefile' nyc_streets lion/BXLION1.tab -nln streets
             * 
             * It also requires the NYC Subway data in GTFS: cd src/test/resources wget
             * http://data.topplabs.org/data/mta_nyct_subway/subway.zip
             */
            return;
        }

        ShapefileFeatureSourceFactoryImpl factory = new ShapefileFeatureSourceFactoryImpl(file);

        ShapefileStreetSchema schema = new ShapefileStreetSchema();
        schema.setIdAttribute("SegmentID");
        schema.setNameAttribute("Street");

        /* only featuretyp=0 are streets */
        CaseBasedBooleanConverter selector = new CaseBasedBooleanConverter("FeatureTyp", false);

        HashMap<String, Boolean> streets = new HashMap<String, Boolean>();
        streets.put("0", true);
        selector.setValues(streets);
        schema.setFeatureSelector(selector);
        
        /* street directions */
        CaseBasedTraversalPermissionConverter perms = new CaseBasedTraversalPermissionConverter(
                "TrafDir", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

        perms.addPermission("W", StreetTraversalPermission.ALL,
                StreetTraversalPermission.PEDESTRIAN);
        perms.addPermission("A", StreetTraversalPermission.PEDESTRIAN,
                StreetTraversalPermission.ALL);
        perms.addPermission("T", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);

        schema.setPermissionConverter(perms);

        ShapefileStreetGraphBuilderImpl loader = new ShapefileStreetGraphBuilderImpl();
        loader.setFeatureSourceFactory(factory);
        loader.setSchema(schema);

        loader.buildGraph(gg);

        //find start and end vertices
        GraphVertex start = null;
        GraphVertex end = null;
        GraphVertex carlton = null;
        for (GraphVertex gv : gg.getVertices()) {
            Vertex v = gv.vertex;
            if (v.getLabel().startsWith("PARK PL at VANDERBILT AV out")) {
                start = gv;
            } else if (v.getLabel().startsWith("GRAND ST at LAFAYETTE ST in")) {
                end = gv;
            } else if (v.getLabel().startsWith("CARLTON AV at PARK PL in")) {
                carlton = gv;
            }
        }
        assertNotNull(start);
        assertNotNull(end);
        assertNotNull(carlton);
        
        assertEquals(4, start.getDegreeOut());
        assertEquals(0, start.getDegreeIn());
        
        GraphVertex sv = null;
        for (Edge e : start.getOutgoing()) {
            sv = gg.getGraphVertex(e.getToVertex());
            break;
        }
        
        assertEquals(4, sv.getDegreeOut());
        assertEquals(4, sv.getDegreeIn());
        
        TraverseOptions wo = new TraverseOptions();
        ShortestPathTree spt = AStar.getShortestPathTree(gg, start.vertex, end.vertex, new State(0), wo);
        assertNotNull(spt);

        //test that the option to walk bikes on the first or last segment works
        
        wo = new TraverseOptions(new TraverseModeSet(TraverseMode.BICYCLE));
        
        //Real live cyclists tell me that they would prefer to ride around the long way than to 
        //walk their bikes the short way.  If we slow down the default biking speed, that will 
        //force a change in preferences.
        wo.speed = 2; 
        
        spt = AStar.getShortestPathTree(gg, start.vertex, carlton.vertex, new State(0), wo);
        assertNotNull(spt);
        
        GraphPath path = spt.getPath(carlton.vertex);
        assertTrue(path.edges.size() <= 3);

        wo.setArriveBy(true);
        spt = AStar.getShortestPathTreeBack(gg, start.vertex, carlton.vertex, new State(0), wo);
        assertNotNull(spt);
        
        path = spt.getPath(carlton.vertex);
        assertTrue(path.edges.size() <= 3);

    }
}
