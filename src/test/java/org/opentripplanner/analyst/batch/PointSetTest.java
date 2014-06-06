package org.opentripplanner.analyst.batch;

import junit.framework.TestCase;
import org.opentripplanner.analyst.PointSet;

import java.io.IOException;

public class PointSetTest extends TestCase {

    public void testPointSets() throws IOException {
        PointSet schools = PointSet.fromCsv("src/test/resources/pointset/schools.csv");
        assertNotNull(schools);
        assertEquals(schools.capacity, 9);
    }

    /** Factory method should return null but not throw an exception on malformed CSV. */
    public void testBogusCSV() throws IOException {
        PointSet points = PointSet.fromCsv("src/test/resources/pointset/bogus.csv");
        assertNull(points);
    }

    public void testLoadGeoJson() {
        PointSet points = PointSet.fromGeoJson("src/test/resources/pointset/population.geo.json");
        assertNotNull(points);
        assertEquals(points.capacity, 2);
    }

    /* TODO Round trip serialization and deserialization to GeoJSON. */

}
