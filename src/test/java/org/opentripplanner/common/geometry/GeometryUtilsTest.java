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

package org.opentripplanner.common.geometry;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opentripplanner.common.model.P2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class GeometryUtilsTest {
    @Test
    public final void testSplitGeometryAtFraction() {
        Coordinate[] coordinates = new Coordinate[4];

        coordinates[0] = new Coordinate(0, 0);
        coordinates[1] = new Coordinate(0, 1);
        coordinates[2] = new Coordinate(2, 1);
        coordinates[3] = new Coordinate(2, 2);

        Coordinate[][][] referenceCoordinates = new Coordinate[9][2][];

        referenceCoordinates[0][1] = coordinates;

        referenceCoordinates[1][0] = new Coordinate[2];
        referenceCoordinates[1][0][0] = coordinates[0];
        referenceCoordinates[1][0][1] = new Coordinate(0, 0.5);
        referenceCoordinates[1][1] = new Coordinate[4];
        referenceCoordinates[1][1][0] = referenceCoordinates[1][0][1];
        referenceCoordinates[1][1][1] = coordinates[1];
        referenceCoordinates[1][1][2] = coordinates[2];
        referenceCoordinates[1][1][3] = coordinates[3];

        referenceCoordinates[2][0] = new Coordinate[2];
        referenceCoordinates[2][0][0] = coordinates[0];
        referenceCoordinates[2][0][1] = coordinates[1];
        referenceCoordinates[2][1] = new Coordinate[3];
        referenceCoordinates[2][1][0] = coordinates[1];
        referenceCoordinates[2][1][1] = coordinates[2];
        referenceCoordinates[2][1][2] = coordinates[3];

        referenceCoordinates[3][0] = new Coordinate[3];
        referenceCoordinates[3][0][0] = coordinates[0];
        referenceCoordinates[3][0][1] = coordinates[1];
        referenceCoordinates[3][0][2] = new Coordinate(0.5, 1);
        referenceCoordinates[3][1] = new Coordinate[3];
        referenceCoordinates[3][1][0] = referenceCoordinates[3][0][2];
        referenceCoordinates[3][1][1] = coordinates[2];
        referenceCoordinates[3][1][2] = coordinates[3];

        referenceCoordinates[4][0] = new Coordinate[3];
        referenceCoordinates[4][0][0] = coordinates[0];
        referenceCoordinates[4][0][1] = coordinates[1];
        referenceCoordinates[4][0][2] = new Coordinate(1, 1);
        referenceCoordinates[4][1] = new Coordinate[3];
        referenceCoordinates[4][1][0] = referenceCoordinates[4][0][2];
        referenceCoordinates[4][1][1] = coordinates[2];
        referenceCoordinates[4][1][2] = coordinates[3];

        referenceCoordinates[5][0] = new Coordinate[3];
        referenceCoordinates[5][0][0] = coordinates[0];
        referenceCoordinates[5][0][1] = coordinates[1];
        referenceCoordinates[5][0][2] = new Coordinate(1.5, 1);
        referenceCoordinates[5][1] = new Coordinate[3];
        referenceCoordinates[5][1][0] = referenceCoordinates[5][0][2];
        referenceCoordinates[5][1][1] = coordinates[2];
        referenceCoordinates[5][1][2] = coordinates[3];

        referenceCoordinates[6][0] = new Coordinate[3];
        referenceCoordinates[6][0][0] = coordinates[0];
        referenceCoordinates[6][0][1] = coordinates[1];
        referenceCoordinates[6][0][2] = coordinates[2];
        referenceCoordinates[6][1] = new Coordinate[2];
        referenceCoordinates[6][1][0] = coordinates[2];
        referenceCoordinates[6][1][1] = coordinates[3];

        referenceCoordinates[7][0] = new Coordinate[4];
        referenceCoordinates[7][0][0] = coordinates[0];
        referenceCoordinates[7][0][1] = coordinates[1];
        referenceCoordinates[7][0][2] = coordinates[2];
        referenceCoordinates[7][0][3] = new Coordinate(2, 1.5);
        referenceCoordinates[7][1] = new Coordinate[2];
        referenceCoordinates[7][1][0] = referenceCoordinates[7][0][3];
        referenceCoordinates[7][1][1] = coordinates[3];

        referenceCoordinates[8][0] = coordinates;

        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        CoordinateSequenceFactory coordinateSequenceFactory =
                geometryFactory.getCoordinateSequenceFactory();
        CoordinateSequence sequence = coordinateSequenceFactory.create(coordinates);
        LineString geometry = new LineString(sequence, geometryFactory);

        P2<LineString> result;
        LineString[][] results = new LineString[9][2];

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0);
        results[0][0] = result.first;
        results[0][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0.125);
        results[1][0] = result.first;
        results[1][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0.25);
        results[2][0] = result.first;
        results[2][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0.375);
        results[3][0] = result.first;
        results[3][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0.5);
        results[4][0] = result.first;
        results[4][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0.625);
        results[5][0] = result.first;
        results[5][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0.75);
        results[6][0] = result.first;
        results[6][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 0.875);
        results[7][0] = result.first;
        results[7][1] = result.second;

        result = GeometryUtils.splitGeometryAtFraction(geometry, 1);
        results[8][0] = result.first;
        results[8][1] = result.second;

        sequence = coordinateSequenceFactory.create(referenceCoordinates[0][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[0][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[0][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[0][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[1][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[1][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[1][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[1][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[2][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[2][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[2][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[2][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[3][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[3][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[3][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[3][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[4][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[4][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[4][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[4][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[5][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[5][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[5][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[5][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[6][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[6][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[6][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[6][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[7][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[7][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[7][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[7][1]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[8][0]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[8][0]);

        sequence = coordinateSequenceFactory.create(referenceCoordinates[8][1]);
        geometry = new LineString(sequence, geometryFactory);
        assertEquals(geometry, results[8][1]);
    }
}
