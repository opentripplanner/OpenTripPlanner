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

package org.opentripplanner.routing.edgetype;

import junit.framework.TestCase;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.util.SlopeCosts;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.util.NonLocalizedString;

public class TestTriangle extends TestCase {

    public void testTriangle() {
        Coordinate c1 = new Coordinate(-122.575033, 45.456773);
        Coordinate c2 = new Coordinate(-122.576668, 45.451426);

        StreetVertex v1 = new IntersectionVertex(null, "v1", c1.x, c1.y, (NonLocalizedString)null);
        StreetVertex v2 = new IntersectionVertex(null, "v2", c2.x, c2.y, (NonLocalizedString)null);

        GeometryFactory factory = new GeometryFactory();
        LineString geometry = factory.createLineString(new Coordinate[] { c1, c2 });

        double length = 650.0;

        StreetWithElevationEdge testStreet = new StreetWithElevationEdge(v1, v2, geometry, "Test Lane", length,
                StreetTraversalPermission.ALL, false);
        testStreet.setBicycleSafetyFactor(0.74f); // a safe street

        Coordinate[] profile = new Coordinate[] { 
                new Coordinate(0, 0), // slope = 0.1
                new Coordinate(length / 2, length / 20.0), 
                new Coordinate(length, 0) // slope = -0.1
        };
        PackedCoordinateSequence elev = new PackedCoordinateSequence.Double(profile);
        testStreet.setElevationProfile(elev, false);

        SlopeCosts costs = ElevationUtils.getSlopeCosts(elev, true);
        double trueLength = costs.lengthMultiplier * length;
        double slopeWorkLength = testStreet.getSlopeWorkCostEffectiveLength();
        double slopeSpeedLength = testStreet.getSlopeSpeedEffectiveLength();

        RoutingRequest options = new RoutingRequest(TraverseMode.BICYCLE);
        options.optimize = OptimizeType.TRIANGLE;
        options.bikeSpeed = 6.0;
        options.walkReluctance = 1;

        options.setTriangleSafetyFactor(0);
        options.setTriangleSlopeFactor(0);
        options.setTriangleTimeFactor(1);
        State startState = new State(v1, options);
        State result = testStreet.traverse(startState);
        double timeWeight = result.getWeight();
        double expectedTimeWeight = slopeSpeedLength / options.getSpeed(TraverseMode.BICYCLE);
        assertTrue(Math.abs(expectedTimeWeight - timeWeight) < 0.00001);

        options.setTriangleSafetyFactor(0);
        options.setTriangleSlopeFactor(1);
        options.setTriangleTimeFactor(0);
        startState = new State(v1, options);
        result = testStreet.traverse(startState);
        double slopeWeight = result.getWeight();
        double expectedSlopeWeight = slopeWorkLength / options.getSpeed(TraverseMode.BICYCLE);
        assertTrue(Math.abs(expectedSlopeWeight - slopeWeight) < 0.00001);
        assertTrue(length * 1.5 / options.getSpeed(TraverseMode.BICYCLE) < slopeWeight);
        assertTrue(length * 1.5 * 10 / options.getSpeed(TraverseMode.BICYCLE) > slopeWeight);

        options.setTriangleSafetyFactor(1);
        options.setTriangleSlopeFactor(0);
        options.setTriangleTimeFactor(0);
        startState = new State(v1, options);
        result = testStreet.traverse(startState);
        double safetyWeight = result.getWeight();
        double slopeSafety = costs.slopeSafetyCost;
        double expectedSafetyWeight = (trueLength * 0.74 + slopeSafety) / options.getSpeed(TraverseMode.BICYCLE);
        assertTrue(Math.abs(expectedSafetyWeight - safetyWeight) < 0.00001);

        final double ONE_THIRD = 1/3.0;
        options.setTriangleSafetyFactor(ONE_THIRD);
        options.setTriangleSlopeFactor(ONE_THIRD);
        options.setTriangleTimeFactor(ONE_THIRD);
        startState = new State(v1, options);
        result = testStreet.traverse(startState);
        double averageWeight = result.getWeight();
        assertTrue(Math.abs(safetyWeight * ONE_THIRD + slopeWeight * ONE_THIRD + timeWeight * ONE_THIRD - averageWeight) < 0.00000001);

    }
}