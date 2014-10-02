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

package org.opentripplanner.routing.util;

import junit.framework.TestCase;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

public class TestElevationUtils extends TestCase {

	public void testLengthMultiplier() {

		PackedCoordinateSequenceFactory factory = PackedCoordinateSequenceFactory.DOUBLE_FACTORY;
		CoordinateSequence seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 1) });
		SlopeCosts costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.0, costs.lengthMultiplier);
		
		seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 2) });
                costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.00498756211208902702, costs.lengthMultiplier);
		
		seq = factory.create(new Coordinate[] {
				new Coordinate(0, 1), new Coordinate(10, 2), new Coordinate(15, 1) });
                costs = ElevationUtils.getSlopeCosts(seq, false);
		assertEquals(1.00992634231424500668, costs.lengthMultiplier);
	}

}
