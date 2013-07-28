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

import java.io.Serializable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;

/**
 * 2D, supports only coordinates in range +/- 180.
 */
public class IntPackedCoordinateSequence implements CoordinateSequence, Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    int[] ordinates;
    
    private static final double TO_FIXED = Integer.MAX_VALUE / 180.0;
    private static final double FROM_FIXED = 180.0 / Integer.MAX_VALUE;
            
    private int toFixedInt(double latlon) {
        return (int) (latlon * TO_FIXED);
    }
    
    private double fromFixedInt(int fixed) {
        return (double) (fixed * FROM_FIXED);
    }
    
    public IntPackedCoordinateSequence(Coordinate[] coordinates) {
        int nc = coordinates.length;
        ordinates = new int[nc * 2];
        int i = 0;
        for (Coordinate c : coordinates) {
            setOrdinate(i, 0, c.x);
            setOrdinate(i, 1, c.y);
            i++;
        }
    }
    
    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public Coordinate getCoordinate(int i) {
        return getCoordinateCopy(i);
    }

    @Override
    public Coordinate getCoordinateCopy(int i) {
        return new Coordinate(getX(i), getY(i));
    }

    @Override
    public void getCoordinate(int index, Coordinate coord) {
        coord.x = getX(index);
        coord.y = getY(index);
    }

    @Override
    public double getX(int index) {
        return fromFixedInt(ordinates[2*index]);
    }

    @Override
    public double getY(int index) {
        return fromFixedInt(ordinates[2*index + 1]);
    }

    @Override
    public double getOrdinate(int index, int ordinateIndex) {
        return fromFixedInt(ordinates[2*index + ordinateIndex]);
    }

    @Override
    public int size() {
        return ordinates.length / 2;
    }

    @Override
    public void setOrdinate(int index, int ordinateIndex, double value) {
        ordinates[2*index + ordinateIndex] = toFixedInt(value);
    }

    @Override
    public Coordinate[] toCoordinateArray() {
        Coordinate[] ret = new Coordinate[this.size()];
        for (int i = 0; i < this.size(); i++) {
            ret[i] = this.getCoordinate(i);
        }
        return ret;
    }

    @Override
    public Envelope expandEnvelope(Envelope env) {
        for (int i = 0; i < ordinates.length / 2; i++) {
            env.expandToInclude(getX(i), getY(i));
        }
        return env;
    }

    @Override
    public IntPackedCoordinateSequence clone() {
        try {
            return (IntPackedCoordinateSequence) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("programming error.", e);
        }
    }
}
