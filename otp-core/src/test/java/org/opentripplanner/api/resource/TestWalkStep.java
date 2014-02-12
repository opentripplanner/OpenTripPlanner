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

package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;

import junit.framework.TestCase;

public class TestWalkStep extends TestCase {

    public void testRelativeDirection() {
        WalkStep step = new WalkStep();

        double angle1 = degreesToRadians(0);
        double angle2 = degreesToRadians(90);

        step.setDirections(angle1, angle2, false);
        assertEquals(RelativeDirection.RIGHT, step.relativeDirection);
        assertEquals(AbsoluteDirection.EAST, step.absoluteDirection);

        angle1 = degreesToRadians(0);
        angle2 = degreesToRadians(5);

        step.setDirections(angle1, angle2, false);
        assertEquals(RelativeDirection.CONTINUE, step.relativeDirection);
        assertEquals(AbsoluteDirection.NORTH, step.absoluteDirection);

        angle1 = degreesToRadians(0);
        angle2 = degreesToRadians(240);

        step.setDirections(angle1, angle2, false);
        assertEquals(RelativeDirection.HARD_LEFT, step.relativeDirection);
        assertEquals(AbsoluteDirection.SOUTHWEST, step.absoluteDirection);

    }

    private double degreesToRadians(double deg) {
        return deg * Math.PI / 180;
    }
}
