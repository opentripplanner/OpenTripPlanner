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
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;

public class Serializable2DPackedCoordinateSequenceFactory implements Serializable, CoordinateSequenceFactory {

    private static final long serialVersionUID = 1L;
    
    @Override
    public CoordinateSequence create(Coordinate[] coordinates) {
        return new PackedCoordinateSequence.Double(coordinates, 2);
        //return new IntPackedCoordinateSequence(coordinates);
    }

    @Override
    public CoordinateSequence create(CoordinateSequence coordSeq) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoordinateSequence create(int size, int dimension) {
        throw new UnsupportedOperationException();
    }
    

}
