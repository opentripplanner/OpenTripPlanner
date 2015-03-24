/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.util;

import com.csvreader.CsvWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static junit.framework.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;

/**
 *
 * @author mabu
 */
public class GeometryCSVWriterTest {

    private static List<Geometry> geometries;

    @BeforeClass
    public static void setUpClass() throws Exception {
        geometries = new ArrayList<>(4);
        geometries.add(GeometryUtils.getGeometryFactory().createPoint(new Coordinate(1, 3.4)));
        geometries.add(GeometryUtils.getGeometryFactory().createPoint(new Coordinate(2, 2.8)));
        geometries.add(GeometryUtils.getGeometryFactory().createPoint(new Coordinate(3, 3.15)));
        geometries.add(GeometryUtils.getGeometryFactory().createPoint(new Coordinate(4, 8.55)));
    }

    /**
     * Test of GeometryCSVWriter with Geometry as last column.
     */
    @Test
    public void testGeoLast() {
        GeometryCSVWriter writer = new GeometryCSVWriter(Arrays.asList("name", "type", "geo"), "geo", new StringWriter());
        writer.add(Arrays.asList("first", "0"), geometries.get(0));
        writer.add(Arrays.asList("second", "1"), geometries.get(1));
        writer.close();
        String expected = "name,type,geo\n"
                + "first,0,POINT (1 3.4)\n"
                + "second,1,POINT (2 2.8)\n";
        //System.out.println(writer.test_writer.toString());
        assertEquals(expected, writer.testWriter.toString());
    }

    /**
     * Test of GeometryCSVWriter with Geometry as second column.
     */
    @Test
    public void testGeoSecond() {
        GeometryCSVWriter writer = new GeometryCSVWriter(Arrays.asList("name", "geo", "type", "height"), "geo", new StringWriter());
        writer.add(Arrays.asList("first", "0", "55"), geometries.get(0));
        writer.add(Arrays.asList("second", "1", "12"), geometries.get(1));
        writer.close();
        //System.out.println(writer.test_writer.toString());
        String expected = "name,geo,type,height\n"
                + "first,POINT (1 3.4),0,55\n"
                + "second,POINT (2 2.8),1,12\n";
        assertEquals(expected, writer.testWriter.toString());
    }

    /**
     * Test of GeometryCSVWriter with Geometry as first column.
     */
    @Test
    public void testGeoFirst() {
        GeometryCSVWriter writer = new GeometryCSVWriter(Arrays.asList("geo", "name", "type", "height"), "geo", new StringWriter());
        writer.add(Arrays.asList("first", "0", "55"), geometries.get(0));
        writer.add(Arrays.asList("second", "1", "12"), geometries.get(1));
        writer.close();
        //System.out.println(writer.test_writer.toString());
        String expected = "geo,name,type,height\n"
                + "POINT (1 3.4),first,0,55\n"
                + "POINT (2 2.8),second,1,12\n";
        assertEquals(expected, writer.testWriter.toString());
    }

    /**
     * Test of GeometryCSVWriter with Geometry as first column. Adding Coordinates.
     */
    @Test
    public void testCoordinateAdd() {
        GeometryCSVWriter writer = new GeometryCSVWriter(Arrays.asList("geo", "name", "type", "height"), "geo", new StringWriter());
        writer.add(Arrays.asList("first", "0", "55"), geometries.get(0).getCoordinate());
        writer.add(Arrays.asList("second", "1", "12"), geometries.get(1).getCoordinate());
        writer.close();
        //System.out.println(writer.test_writer.toString());
        String expected = "geo,name,type,height\n"
                + "POINT (1 3.4),first,0,55\n"
                + "POINT (2 2.8),second,1,12\n";
        assertEquals(expected, writer.testWriter.toString());
    }

    /**
     * Test of GeometryCSVWriter with Geometry as first column and using external csvwriter.
     */
    @Test
    public void testExternalCsvWriter() {
        StringWriter testWriter = new StringWriter();
        CsvWriter csvWriter = new CsvWriter(testWriter, ',');
        GeometryCSVWriter writer = new GeometryCSVWriter(Arrays.asList("geo", "name", "type", "height"), "geo", csvWriter);
        writer.add(Arrays.asList("first", "0", "55"), geometries.get(0));
        writer.add(Arrays.asList("second", "1", "12"), geometries.get(1));
        writer.close();
        //System.out.println(writer.test_writer.toString());
        String expected = "geo,name,type,height\n"
                + "POINT (1 3.4),first,0,55\n"
                + "POINT (2 2.8),second,1,12\n";
        assertEquals(expected, testWriter.toString());
    }
}
