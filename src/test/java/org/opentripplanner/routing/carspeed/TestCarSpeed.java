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

package org.opentripplanner.routing.carspeed;

import junit.framework.TestCase;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.carspeed.CarSpeedSnapshot.StreetEdgeConstantCarSpeedProvider;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class TestCarSpeed extends TestCase {

    private Graph graph;

    private StreetVertex v1;

    private StreetVertex v2;

    private StreetEdge e1;

    @Override
    protected void setUp() throws Exception {
        graph = new Graph();
        v1 = new IntersectionVertex(graph, "v1", 0, 0, "v1");
        v2 = new IntersectionVertex(graph, "v2", 0, 0.01, "v2");
        double len = SphericalDistanceLibrary.fastDistance(v1.getCoordinate(), v2.getCoordinate());
        e1 = new StreetEdge(v1, v2, null, "Demo", len, StreetTraversalPermission.ALL, false);
    }

    /**
     * Test that a car speed snapshot is indeed a consistent view.
     */
    public void testCarSpeedSnapshotIsConsistent() throws Exception {

        CarSpeedSnapshotSource carSpeedSnapshotSource = new CarSpeedSnapshotSource();
        CarSpeedSnapshot snapshot = carSpeedSnapshotSource.getCarSpeedSnapshot();

        // Initial state: no data, should return the default value
        float carSpeed = snapshot.getCarSpeed(e1, 0L, 42.0f);
        assertEquals(42.0f, carSpeed);

        carSpeedSnapshotSource.updateCarSpeedProvider(e1, new StreetEdgeConstantCarSpeedProvider(
                24.0f));
        // The snapshot should still return the default value, even after the update
        carSpeed = snapshot.getCarSpeed(e1, 0L, 42.0f);
        assertEquals(42.0f, carSpeed);

        // With a new snapshot, we still have the old value, because it's not commited
        snapshot = carSpeedSnapshotSource.getCarSpeedSnapshot();
        carSpeed = snapshot.getCarSpeed(e1, 0L, 42.0f);
        assertEquals(42.0f, carSpeed);

        // A commit on a old snapshot will still return the old value
        carSpeedSnapshotSource.commit();
        carSpeed = snapshot.getCarSpeed(e1, 0L, 42.0f);
        assertEquals(42.0f, carSpeed);

        // A commit with a new snapshot will be updated
        snapshot = carSpeedSnapshotSource.getCarSpeedSnapshot();
        carSpeed = snapshot.getCarSpeed(e1, 0L, 42.0f);
        assertEquals(24.0f, carSpeed);

        // Test removal
        carSpeedSnapshotSource.updateCarSpeedProvider(e1, null);
        carSpeedSnapshotSource.commit();
        // Still the old value: no new snapshot
        carSpeed = snapshot.getCarSpeed(e1, 0L, 42.0f);
        assertEquals(24.0f, carSpeed);

        // New snapshot, no value -> fallback to default value
        snapshot = carSpeedSnapshotSource.getCarSpeedSnapshot();
        carSpeed = snapshot.getCarSpeed(e1, 0L, 42.0f);
        assertEquals(42.0f, carSpeed);
    }

    /**
     * Basic routing test with dynamic car speed source.
     */
    public void testRouting() throws Exception {

        AStar aStar = new AStar();

        final float STATIC_CAR_SPEED = 20.0f;
        final float DYNAMIC_CAR_SPEED = 20.0f;
        final double DISTANCE = e1.getDistance();
        e1.setCarSpeed(STATIC_CAR_SPEED);

        graph.carSpeedSnapshotSource = new CarSpeedSnapshotSource();

        // Going from v1 to v2 should take t=d/v
        RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.CAR));
        options.setRoutingContext(graph, v1, v2);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(v2, false);
        assertEquals(Math.round(DISTANCE / STATIC_CAR_SPEED), path.getDuration());

        graph.carSpeedSnapshotSource.updateCarSpeedProvider(e1,
                new StreetEdgeConstantCarSpeedProvider(DYNAMIC_CAR_SPEED));
        graph.carSpeedSnapshotSource.commit();

        // Same static speed, using the snapshot from the origin request
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v2, false);
        assertEquals(Math.round(DISTANCE / STATIC_CAR_SPEED), path.getDuration());

        // Now we should be using the dynamic speed
        options = new RoutingRequest(new TraverseModeSet(TraverseMode.CAR));
        options.setRoutingContext(graph, v1, v2);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v2, false);
        assertNotNull(path);
        assertEquals(Math.round(DISTANCE / DYNAMIC_CAR_SPEED), path.getDuration());
    }
}
