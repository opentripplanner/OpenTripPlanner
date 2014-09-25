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

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;

public class HashGridTest extends TestCase {

    private static class DummyObject {
        Envelope envelope;

        @Override
        public String toString() {
            return envelope.toString();
        }
    }

    /**
     * We perform a non-regression random test. We insert many random-envelop objects
     * into both a hash grid (OTP) and STRtree (JTS) spatial indexes. We check with
     * many random query that the set of returned objects is the same (after pruning
     * because both could return false positives).
     */
    @SuppressWarnings("unchecked")
    public void testHashGridRandom() {
        final double X0 = -0.05;
        final double Y0 = 44.0;
        final double DX = 0.1;
        final double DY = 0.1;
        final int N_OBJS = 1000;
        final int N_QUERIES = 1000;

        Random rand = new Random(42);
        SpatialIndex hashGrid = new HashGridSpatialIndex<>();
        SpatialIndex strTree = new STRtree();

        for (int i = 0; i < N_OBJS; i++) {
            Coordinate a = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
            Coordinate b = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
            DummyObject obj = new DummyObject();
            obj.envelope = new Envelope(a, b);
            hashGrid.insert(obj.envelope, obj);
            strTree.insert(obj.envelope, obj);
        }

        for (int i = 0; i < N_QUERIES; i++) {
            Coordinate a = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
            Coordinate b = new Coordinate(rand.nextDouble() * DX + X0, rand.nextDouble() * DY + Y0);
            Envelope searchEnv = new Envelope(a, b);
            List<DummyObject> hashGridObjs = hashGrid.query(searchEnv);
            // Need to remove non intersecting
            Set<DummyObject> hashGridObjs2 = new HashSet<>();
            for (DummyObject obj : hashGridObjs) {
                if (obj.envelope.intersects(searchEnv))
                    hashGridObjs2.add(obj);
            }
            List<DummyObject> strtreeObjs = hashGrid.query(searchEnv);
            // Need to remove non intersecting
            Set<DummyObject> strtreeObjs2 = new HashSet<>();
            for (DummyObject obj : strtreeObjs) {
                if (obj.envelope.intersects(searchEnv))
                    strtreeObjs2.add(obj);
            }
            boolean equals = hashGridObjs2.equals(strtreeObjs2);
            assertTrue(equals);
        }

    }
}