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

/**
 * An object sent back to a client through a web-service which contains sample points in a regular
 * grid about transit times.
 * 
 * The structure of the returned data has the following constraints: 1) It must be fast to generate
 * and serialize, and not consume too much bandwith once serialized (JSON) 2) It must be easily
 * processed by the client who receive it (ie easy to index).
 * 
 * @author laurent
 */
public interface TimeGrid {

    public interface TimeGridBlock {

        public int getX0();

        public int getY0();

        /**
         * @return An [x][y] arrays of values {time,distance} for the block.
         */
        public int[][][] getTimePoints();
    }

    /** @return The geographical reference longitude (index=0 correspond to this point) */
    public double getLonCenter();

    /** @return The geographical reference latitude (index=0 correspond to this point) */
    public double getLatCenter();

    /** @return The longitude size of a grid cell. */
    public double getLonDelta();

    /** @return The latitude size of a grid cell. */
    public double getLatDelta();

    /** @return The size of each block in X. */
    public int getBlockSizeX();

    /** @return The size of each block in Y. */
    public int getBlockSizeY();

    /** @return The max off-road distance to consider. */
    public int getOffRoadDistanceMeters();

    /** @return An iterator on all returned blocks. */
    public Iterable<TimeGridBlock> getBlocks();
}
