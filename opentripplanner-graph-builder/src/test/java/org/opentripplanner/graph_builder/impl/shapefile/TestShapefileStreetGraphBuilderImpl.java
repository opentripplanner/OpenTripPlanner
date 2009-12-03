/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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

import junit.framework.TestCase;

import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

public class TestShapefileStreetGraphBuilderImpl extends TestCase {

    public void testBasic() throws Exception {
        Graph gg = new Graph();

        File file = new File("src/test/resources/nyc_streets/streets.shp");

        if (!file.exists()) {
            System.out.println("No New York City basemap; skipping; see comment here for details");
            /*
             * This test requires the New York City base map, available at:
             * http://www.nyc.gov/html/dcp/html/bytes/dwnlion.shtml Download the MapInfo file. This
             * must be converted to a ShapeFile. unzip nyc_lion09ami.zip ogr2ogr -f 'ESRI Shapefile'
             * nyc_streets/streets.shp lion/MNLION1.tab ogr2ogr -update -append -f 'ESRI Shapefile'
             * nyc_streets lion/SILION1.tab -nln streets ogr2ogr -update -append -f 'ESRI Shapefile'
             * nyc_streets lion/QNLION1.tab -nln streets ogr2ogr -update -append -f 'ESRI Shapefile'
             * nyc_streets lion/BKLION1.tab -nln streets ogr2ogr -update -append -f 'ESRI Shapefile'
             * nyc_streets lion/BXLION1.tab -nln streets
             * 
             * It also requires the NYC Subway data in GTFS: cd src/test/resources wget
             * http://data.topplabs.org/data/mta_nyct_subway/subway.zip
             */
            return;
        }

        ShapefileFeatureSourceFactoryImpl factory = new ShapefileFeatureSourceFactoryImpl(file);

        ShapefileStreetSchema schema = new ShapefileStreetSchema();
        schema.setIdAttribute("StreetCode");
        schema.setNameAttribute("Street");

        CaseBasedTraversalPermissionConverter perms = new CaseBasedTraversalPermissionConverter(
                "TrafDir", StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE_ONLY);

        perms.addPermission("W", StreetTraversalPermission.ALL,
                StreetTraversalPermission.PEDESTRIAN_ONLY);
        perms.addPermission("A", StreetTraversalPermission.PEDESTRIAN_ONLY,
                StreetTraversalPermission.ALL);
        perms.addPermission("T", StreetTraversalPermission.ALL, StreetTraversalPermission.ALL);

        schema.setPermissionConverter(perms);

        ShapefileStreetGraphBuilderImpl loader = new ShapefileStreetGraphBuilderImpl();
        loader.setFeatureSourceFactory(factory);
        loader.setSchema(schema);

        loader.buildGraph(gg);

        assertEquals(104910, gg.getVertices().size());

        Vertex start = gg.getVertex("PARK PL at VANDERBILT AV");
        Vertex end = gg.getVertex("GRAND ST at LAFAYETTE ST");

        assertNotNull(start);
        assertNotNull(end);
    }
}
