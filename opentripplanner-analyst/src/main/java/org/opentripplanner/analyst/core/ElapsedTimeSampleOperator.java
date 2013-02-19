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

package org.opentripplanner.analyst.core;

import org.opentripplanner.routing.core.State;

/**
 * The basic sample operator: minimize elapsed time with no initial wait removal or path 
 * optimization.  
 */
public class ElapsedTimeSampleOperator extends SampleOperator {

    public static final double DEFAULT_WALK_SPEED = 3.33; // m/sec

    private double walkSpeed;

    public ElapsedTimeSampleOperator() {
        this(DEFAULT_WALK_SPEED);
    }

    public ElapsedTimeSampleOperator(double walkSpeed) {
        this.walkSpeed = walkSpeed;
        this.minimize = true;
    }

    @Override
    public int evaluate(State state, double distance) {
        int seconds = (int) (distance / walkSpeed);
        return seconds + (int) state.getElapsedTime();
    }

}
